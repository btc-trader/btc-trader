package com.bitcoincharts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import org.chartsy.main.data.DataItem;
import org.chartsy.main.data.DataProvider;
import org.chartsy.main.data.Dataset;
import org.chartsy.main.data.Stock;
import org.chartsy.main.data.StockNode;
import org.chartsy.main.data.StockSet;
import org.chartsy.main.exceptions.InvalidStockException;
import org.chartsy.main.exceptions.RegistrationException;
import org.chartsy.main.exceptions.StockNotFoundException;
import org.chartsy.main.intervals.Interval;
import org.chartsy.main.managers.CacheManager;
import org.chartsy.main.managers.DatasetUsage;
import org.chartsy.main.managers.ProxyManager;
import org.chartsy.main.utils.SerialVersion;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openide.util.NbBundle;

/**
 *
 * @author coinfreak
 */
public final class BitcoinChartsDataProvider extends DataProvider implements Runnable
{
    private static final long serialVersionUID = SerialVersion.APPVERSION;

    private static final Interval[] SUPPORTED_INTERVALS = {
        ONE_MINUTE, FIVE_MINUTE, FIFTEEN_MINUTE, THIRTY_MINUTE, SIXTY_MINUTE,
        DAILY, WEEKLY, MONTHLY
    };

    private final JSONParser parser;

    private HashMap<String,String> symbolMap = null;
    private HashMap<String,Dataset> liveData = null;
    private long liveSince = 0;

    public BitcoinChartsDataProvider()
    {
        super(NbBundle.getBundle(BitcoinChartsDataProvider.class), true, false);
        this.parser = new JSONParser();
    }

    @Override
    public int getRefreshInterval()
    {
        return 1;
    }

    @Override
    public Interval[] getSupportedIntervals()
    {
        return SUPPORTED_INTERVALS;
    }

    @Override
    public void initialize()
    {
        synchronized (parser)
        {
            try {
                BufferedReader rd = ProxyManager.getDefault().bufferReaderGET(getMarketsUrl());
                JSONArray data = null;
                try {
                    data = (JSONArray)( parser.parse(rd) );
                }
                catch (org.json.simple.parser.ParseException e) {
                    throw new IOException(e);
                }
                finally {
                    rd.close();
                }

                symbolMap = new HashMap<String,String>();
                liveData = new HashMap<String,Dataset>();

                for ( ListIterator iter = data.listIterator(); iter.hasNext(); ) {
                    JSONObject market = (JSONObject)( iter.next() );

                    String symbol = (String)( market.get("symbol") );
                    symbolMap.put( symbol.toUpperCase(), symbol );
                    liveData.put( symbol.toUpperCase(), new Dataset() );
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Thread liveFeed = new Thread( this, BitcoinChartsDataProvider.class.getSimpleName() );
        liveFeed.setDaemon(true);
        liveFeed.start();
    }

    @Override
    public void run()
    {
        boolean running = true;

        try {
            while (running) {
                Socket s = new Socket( getLiveFeedAddress(), getLiveFeedPort() );
                BufferedReader rd = new BufferedReader( new InputStreamReader( s.getInputStream() ) );

                HashMap<String,Dataset> newLiveData = null;
                long newLiveSince = 0;

                try {
                    String line = null;
                    while ( (line = rd.readLine()) != null )
                    {
                        // Crop \0
                        if ( line.charAt(0) == 0 )
                            line = line.substring(1);

                        synchronized (parser)
                        {
                            JSONObject tick;
                            try {
                                tick = (JSONObject)( parser.parse(line) );
                            }
                            catch (org.json.simple.parser.ParseException e) {
                                // TODO log this
                                continue;
                            }

                            String symbol = (String)tick.get("symbol");
                            long time = ((Number)tick.get("timestamp")).longValue();
                            double price = ((Number)tick.get("price")).doubleValue();
                            double volume = ((Number)tick.get("volume")).doubleValue();

                            if ( newLiveData == null ) {
                                newLiveSince = 1000 * ( time + 1 );
                                newLiveData = new HashMap<String,Dataset>();
                                for ( String k : liveData.keySet() ) {
                                    newLiveData.put( k, new Dataset() );
                                }
                            }

                            if ( 1000*time < newLiveSince )
                                continue;

                            if ( liveSince != newLiveSince ) {
                                liveSince = newLiveSince;
                                liveData = newLiveData;
                            }

                            liveData.get(symbol.toUpperCase()).addDataItem( new DataItem(1000*time,price,price,price,price,volume) );
                        }
                    }
                }
                catch (IOException e) {
                    // TODO log this
                    continue;
                }
                finally {
                    s.close();
                }
            }
        }
        catch (IOException e) {
            running = false;
        }
    }

    @Override
    protected String fetchCompanyName(String symbol)
            throws InvalidStockException, StockNotFoundException, RegistrationException, IOException
    {
        return symbol;
    }

    @Override
    public StockSet fetchAutocomplete(String text)
            throws IOException
    {
        StockSet result = new StockSet();

        for ( String symbol : symbolMap.keySet() ) {
            if ( symbol.startsWith(text) )
                result.add( new StockNode(symbol,symbol,null) );
        }

        return result;
    }

    @Override
    protected Dataset fetchDataForFavorites(Stock stock)
            throws IOException, ParseException
    {
        return fetchData(stock, DAILY);
    }

    @Override
    protected Dataset fetchData(Stock stock, Interval interval)
            throws IOException, ParseException
    {
        synchronized ( (stock.toString() + "-" + interval.getTimeParam()).intern() )
        {
            String fileName = getDatasetKey(stock, interval);
            if ( !datasetExists(stock, interval) ) {
                fetchHistory(stock);
            }
            if ( !DatasetUsage.getInstance().isDatasetInMemory(fileName) ) {
                CacheManager.getInstance().fetchDatasetFromCache(fileName);
            }

            Dataset data = DatasetUsage.getInstance().getDatasetFromMemory(fileName);
            if ( !interval.isIntraDay() )
                return data;

            long newTime = data.getLastTime() + 1000 * interval.getLengthInSeconds();
            if ( newTime > liveSince )
                return data;

            String url = getHistoryUrl(stock.getSymbol(), newTime/1000);
            List<DataItem> ticks = new ArrayList<DataItem>();

            BufferedReader rd = ProxyManager.getDefault().bufferReaderGET(url);
            try {
                String inputLine;
                while ( (inputLine = rd.readLine()) != null )
                {
                    String[] values = inputLine.split(",");

                    long time = Long.parseLong(values[0]);
                    double price = Double.parseDouble(values[1]);
                    double amount = Double.parseDouble(values[2]);

                    if ( amount == 0 )
                        continue;

                    if ( 1000*time > liveSince )
                        break;

                    ticks.add( new DataItem( 1000*time, price, price, price, price, amount ) );
                }
            }
            finally {
                rd.close();
            }

            List<DataItem> bars = aggregateTicks(ticks,interval);
            if ( !bars.isEmpty() ) bars = bars.subList(0, bars.size()-1);
            for ( DataItem bar : bars ) {
                data.addDataItem(bar);
            }

            return data;
        }
    }

    private void fetchHistory(Stock stock)
            throws IOException, ParseException
    {
        HashMap<Interval,ArrayList<DataItem>> history = new HashMap<Interval,ArrayList<DataItem>>();
        for ( Interval i : SUPPORTED_INTERVALS ) {
            history.put( i, new ArrayList<DataItem>() );
        }

        String url = getHistoryUrl(stock.getSymbol(),0);
        BufferedReader rd = ProxyManager.getDefault().bufferReaderGET(url);

        try {
            String inputLine;
            while ( (inputLine = rd.readLine()) != null )
            {
                String[] values = inputLine.split(",");

                long time = Long.parseLong(values[0]);
                double price = Double.parseDouble(values[1]);
                double amount = Double.parseDouble(values[2]);

                if ( amount == 0 )
                    continue;

                for ( Interval i : SUPPORTED_INTERVALS ) {
                    ArrayList<DataItem> data = history.get(i);
                    DataItem last = data.isEmpty() ? null : data.get(data.size()-1);
                    long lastTime = 1000 * ( time - time % i.getLengthInSeconds() );
                    if ( last == null || last.getTime() < lastTime ) {
                        data.add( new DataItem(lastTime, price, price, price, price, amount) );
                    } else {
                        if (last.getHigh() < price) last.setHigh(price);
                        if (last.getLow() > price) last.setLow(price);
                        last.setClose(price);
                        last.setVolume(last.getVolume() + amount);
                    }
                }
            }
        }
        finally {
            rd.close();
        }

        for ( Interval i : SUPPORTED_INTERVALS ) {
            synchronized ( (stock.toString() + "-" + i.getTimeParam()).intern() )
            {
                String fileName = getDatasetKey(stock,i);
                ArrayList<DataItem> data = history.get(i);
                int count = data.size();
                CacheManager.getInstance().cacheDataset(
                        new Dataset( i.isIntraDay() ? data.subList(0, count-1) : data ),
                        fileName,
                        true );
            }
        }
    }

    @Override
    public List<DataItem> getLastDataItems(Stock stock, Interval interval)
    {
        if ( !interval.isIntraDay() || liveSince == 0 )
            return new ArrayList<DataItem>();

        synchronized ( (stock.toString() + "-" + interval.getTimeParam()).intern() )
        {
            String fileName = getDatasetKey(stock, interval);
            if ( !DatasetUsage.getInstance().isDatasetInMemory(fileName)
                || DatasetUsage.getInstance().getDatasetFromMemory(fileName)
                    .getLastTime() < liveSince - 1000 * interval.getLengthInSeconds() )
            {
                try {
                    fetchData(stock, interval);
                }
                catch (IOException e) {
                    // TODO log this
                }
                catch (ParseException e) {
                    // TODO log this
                }
            }

            List<DataItem> newTicks = new ArrayList<DataItem>();
            synchronized (parser)
            {
                newTicks = liveData.get(stock.getSymbol()).getDataItems();
                liveData.put( stock.getSymbol(), new Dataset() );
            }

            List<DataItem> result = new ArrayList<DataItem>();
            for ( Interval i : SUPPORTED_INTERVALS ) {
                List<DataItem> bars = aggregateTicks( newTicks, i );
                synchronized ( (stock.toString() + "-" + i.getTimeParam()).intern() ) {
                    updateIntraDay( getDatasetKey(stock,i), bars );
                }
                if ( interval.equals(i) )
                    result = bars;
            }

            return result;
        }
    }

    private List<DataItem> aggregateTicks( List<DataItem> ticks, Interval interval ) {
        List<DataItem> bars = new ArrayList<DataItem>();
        DataItem b = null;

        for ( DataItem t : ticks ) {
            long time_s = t.getTime() / 1000;
            long barTime_ms = 1000 * ( time_s - time_s % interval.getLengthInSeconds() );

            if ( bars.size() > 0 && b.getTime() == barTime_ms ) {
                if ( t.getHigh() > b.getHigh() ) b.setHigh( t.getHigh() );
                if ( t.getLow() < b.getLow() ) b.setLow( t.getLow() );

                b.setVolume( b.getVolume() + t.getVolume() );
                b.setClose( t.getClose() );
            } else {
                b = new DataItem( barTime_ms, t.getOpen(), t.getHigh(), t.getLow(), t.getClose(), t.getVolume() );
                bars.add(b);
            }
        }

        return bars;
    }

    @Override
    public boolean updateIntraDay( String key, List<DataItem> bars )
    {
        if ( bars.isEmpty() )
            return false;

        // FIXME Buggy
        if ( !DatasetUsage.getInstance().isDatasetInMemory(key) ) {
            try {
                CacheManager.getInstance().fetchDatasetFromCache(key);
            }
            catch (IOException e) {
                return false;
            }
        }

        Dataset ds = DatasetUsage.getInstance().getDatasetFromMemory(key);
        DataItem firstUpdated = bars.get(0);

        int idx;
        for ( idx = ds.getItemsCount(); idx > 0; idx-- ) {
            if ( ds.getDataItem(idx-1).getTime() < firstUpdated.getTime() )
                break;
        }

        for ( DataItem di : bars ) {
            DataItem dj;
            if ( idx < ds.getItemsCount() ) {
                dj = ds.getDataItem(idx);
                if ( di.getHigh() > dj.getHigh() ) dj.setHigh(di.getHigh());
                if ( di.getLow() < dj.getLow() ) dj.setLow(di.getLow());
                dj.setClose(di.getClose());
                dj.setVolume(dj.getVolume() + di.getVolume());
            } else {
                dj = di;
                ds.addDataItem(dj);
            }
            idx++;
        }

        return true;
    }

    @Override
    protected DataItem fetchLastDataItem(Stock stock, Interval interval)
            throws IOException, ParseException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private String getMarketsUrl() {
        return "http://bitcoincharts.com/t/markets.json";
    }

    private String getHistoryUrl(String symbol) {
        return "http://bitcoincharts.com/t/trades.csv?symbol=" + symbolMap.get(symbol);
    }

    private String getHistoryUrl(String symbol, long start) {
        return getHistoryUrl(symbol) + "&start=" + start;
    }

    private String getHistoryUrl(String symbol, long start, long end) {
        return getHistoryUrl(symbol) + "&start=" + start
                                     + "&end="   + end;
    }

    private String getLiveFeedAddress() {
        return "bitcoincharts.com";
    }

    private int getLiveFeedPort() {
        return 27007;
    }
}
