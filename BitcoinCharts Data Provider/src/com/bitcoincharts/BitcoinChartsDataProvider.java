package com.bitcoincharts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
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

    private static final int HEARTBEAT_TIMEOUT = 10000; // ms

    private final JSONParser parser;
    private final HashMap<String,String> symbolMap;
    private final HashMap<String,Long> lastTicks;
    private final HashMap<String,Long> cachedTill;

    public BitcoinChartsDataProvider()
    {
        super(NbBundle.getBundle(BitcoinChartsDataProvider.class), true, false);
        parser = new JSONParser();
        symbolMap = new HashMap<String,String>();
        lastTicks = new HashMap<String,Long>();
        cachedTill = new HashMap<String,Long>();
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

            for ( ListIterator iter = data.listIterator(); iter.hasNext(); ) {
                JSONObject market = (JSONObject)( iter.next() );

                String symbol = (String)( market.get("symbol") );
                symbolMap.put( symbol.toUpperCase(), symbol );
                lastTicks.put( symbol.toUpperCase(), Long.valueOf(0) );
            }
        }
        catch (IOException e) {
            // TODO log this
            return;
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
                for ( String s : lastTicks.keySet() ) {
                    lastTicks.put( s, Long.valueOf(0) );
                }

                Socket s = new Socket( getLiveFeedAddress(), getLiveFeedPort() );
                BufferedReader rd = new BufferedReader( new InputStreamReader( s.getInputStream() ) );
                BufferedWriter wr = new BufferedWriter( new OutputStreamWriter( s.getOutputStream() ) );

                s.setSoTimeout(HEARTBEAT_TIMEOUT);

                try {
                    wr.write("{\"action\":\"subscribe\",\"channel\":\"tick\"}\r\n");
                    wr.flush();

                    while (running) {
                        String line;
                        try {
                            if ( ( line = rd.readLine() ) == null )
                                break;
                        }
                        catch (SocketTimeoutException e) {
                            wr.write("\r\n");
                            wr.flush();
                            continue;
                        }

                        JSONObject obj;
                        try {
                            obj = (JSONObject)( parser.parse(line) );
                        }
                        catch (org.json.simple.parser.ParseException e) {
                            // TODO log this
                            continue;
                        }

                        if ( !((String)obj.get("channel")).startsWith("tick") )
                            continue;

                        JSONObject tick = (JSONObject)obj.get("payload");
                        String symbol = ((String)tick.get("symbol")).toUpperCase();
                        long time = ((Number)tick.get("timestamp")).longValue();
                        double price = ((Number)tick.get("price")).doubleValue();
                        double volume = ((Number)tick.get("volume")).doubleValue();

                        Stock stock = new Stock(symbol);
                        stock.setCompanyName(symbol);

                        synchronized ( (stock.toString() + "-" + ONE_MINUTE.getTimeParam()).intern() )
                        {
                            if ( lastTicks.get(symbol).longValue() > 1000*time )
                                continue;

                            lastTicks.put( symbol, Long.valueOf(1000*time) );

                            String minutesName = getDatasetKey(stock, ONE_MINUTE);
                            if ( DatasetUsage.getInstance().isDatasetInMemory(minutesName) )
                            {
                                Dataset minutes = DatasetUsage.getInstance().getDatasetFromMemory(minutesName);

                                long barTime = 1000 * ( time - time % ONE_MINUTE.getLengthInSeconds() );
                                if ( barTime == minutes.getLastTime() ) {
                                    DataItem bar = minutes.getLastDataItem();
                                    if ( bar.getHigh() == 0 || bar.getHigh() < price ) bar.setHigh(price);
                                    if ( bar.getLow() == 0 || bar.getLow() > price ) bar.setLow(price);
                                    bar.setClose(price);
                                    bar.setVolume(bar.getVolume() + volume);
                                }
                                else if ( barTime > minutes.getLastTime() ) {
                                    minutes.addDataItem( new DataItem(barTime,price,price,price,price,volume) );
                                }
                            }
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
        synchronized ( (stock.toString() + "-" + ONE_MINUTE.getTimeParam()).intern() )
        {
            if ( !symbolMap.containsKey(stock.getSymbol()) )
                return null;

            String minutesName = getDatasetKey(stock, ONE_MINUTE);
            if ( !datasetExists(stock, ONE_MINUTE) ) {
                fetchHistory(stock);
            }

            Dataset minutes = DatasetUsage.getInstance().getDatasetFromMemory(minutesName);
            long fetchTill = lastTicks.get(stock.getSymbol()).longValue();
            long fetchSince;
            if ( minutes == null ) {
                CacheManager.getInstance().fetchDatasetFromCache(minutesName);
                DatasetUsage.getInstance().fetchDataset(minutesName);
                minutes = DatasetUsage.getInstance().getDatasetFromMemory(minutesName);
                fetchSince = minutes.getLastTime() + 1000 * ONE_MINUTE.getLengthInSeconds();
                cachedTill.put( minutesName, Long.valueOf(fetchSince) );
            } else {
                fetchSince = cachedTill.get(minutesName).longValue();
            }

            List<DataItem> ticks = new ArrayList<DataItem>();
            String url = getHistoryUrl(stock.getSymbol(), fetchSince/1000);
            BufferedReader rd = ProxyManager.getDefault().bufferReaderGET(url);
            try {
                String inputLine;
                while ( (inputLine = rd.readLine()) != null ) {
                    String[] values = inputLine.split(",");

                    long time = Long.parseLong(values[0]);
                    double price = Double.parseDouble(values[1]);
                    double amount = Double.parseDouble(values[2]);

                    if ( amount == 0 )
                        continue;

                    if ( 1000*time > fetchTill && fetchTill > 0 )
                        break;

                    ticks.add( new DataItem( 1000*time, price, price, price, price, amount ) );
                }
            }
            finally {
                rd.close();
            }

            if ( fetchTill == 0 && !ticks.isEmpty() ) {
                lastTicks.put( stock.getSymbol(), ticks.get(ticks.size()-1).getTime() );
            }

            List<DataItem> bars = aggregateTicks(ticks,ONE_MINUTE);
            if ( !bars.isEmpty() )
            {
                int idx = minutes.getItemsCount();
                for ( ; idx > 0; idx-- ) {
                    if ( minutes.getTimeAt(idx-1) < fetchSince )
                        break;
                }

                if ( bars.size() > 1 )
                {
                    for ( DataItem b : bars.subList(0, bars.size()-1) ) {
                        if ( idx > minutes.getLastIndex() ) {
                            minutes.addDataItem(b);
                        } else {
                            DataItem i = minutes.getDataItem(idx);
                            i.setTime( b.getTime() );
                            i.setOpen( b.getOpen() );
                            i.setHigh( b.getHigh() );
                            i.setLow ( b.getLow()  );
                            i.setClose( b.getClose() );
                            i.setVolume( b.getVolume() );
                        }
                        idx++;
                    }

                    Dataset cached = new Dataset( minutes.getDataItems().subList(0, idx) );
                    CacheManager.getInstance().cacheDataset( cached, minutesName, true );

                    long newCachedTill = cached.getLastTime() + 1000 * ONE_MINUTE.getLengthInSeconds();
                    cachedTill.put( minutesName, newCachedTill );
                }

                DataItem b = bars.get(bars.size()-1);
                if ( minutes.getLastTime() < b.getTime() )
                    minutes.addDataItem(b);
            }

            if ( interval.equals(ONE_MINUTE) )
                return minutes;

            synchronized ( (stock.toString() + "-" + interval.getTimeParam()).intern() )
            {
                String dataName = getDatasetKey(stock, interval);
                Dataset data = DatasetUsage.getInstance().getDatasetFromMemory(dataName);
                if ( data == null ) {
                    CacheManager.getInstance().fetchDatasetFromCache(dataName);
                    data = DatasetUsage.getInstance().getDatasetFromMemory(dataName);
                    fetchSince = data.getLastTime() + 1000 * interval.getLengthInSeconds();
                    cachedTill.put( dataName, Long.valueOf(fetchSince) );
                } else {
                    fetchSince = cachedTill.get(dataName).longValue();
                }

                int idx = minutes.getItemsCount();
                for ( ; idx > 0; idx-- ) {
                    if ( minutes.getDataItem(idx-1).getTime() < fetchSince )
                        break;
                }

                bars = aggregateTicks(
                        minutes.getDataItems().subList(idx, minutes.getItemsCount()),
                        interval );

                if ( !bars.isEmpty() )
                {
                    idx = data.getItemsCount();
                    for ( ; idx > 0; idx-- ) {
                        if ( data.getDataItem(idx-1).getTime() < fetchSince )
                            break;
                    }

                    if ( bars.size() > 1 )
                    {
                        for ( DataItem b : bars.subList(0, bars.size()-1) ) {
                            if ( idx > data.getLastIndex() ) {
                                data.addDataItem(b);
                            } else {
                                DataItem i = data.getDataItem(idx);
                                i.setTime( b.getTime() );
                                i.setOpen( b.getOpen() );
                                i.setHigh( b.getHigh() );
                                i.setLow ( b.getLow()  );
                                i.setClose( b.getClose() );
                                i.setVolume( b.getVolume() );
                            }
                            idx++;
                        }

                        Dataset cached = new Dataset( data.getDataItems().subList(0, idx) );
                        CacheManager.getInstance().cacheDataset( cached, dataName, true );

                        long newCachedTill = cached.getLastTime() + 1000 * interval.getLengthInSeconds();
                        cachedTill.put( dataName, newCachedTill );
                    }

                    DataItem b = bars.get(bars.size()-1);
                    if ( data.getLastTime() < b.getTime() )
                        data.addDataItem(b);
                }

                return data;
            }
        }
    }

    private void fetchHistory(Stock stock)
            throws IOException, ParseException
    {
        String url = getHistoryUrl(stock.getSymbol(),0);
        BufferedReader rd = ProxyManager.getDefault().bufferReaderGET(url);

        ArrayList<DataItem> minutes = new ArrayList<DataItem>();

        try {
            String inputLine;
            while ( (inputLine = rd.readLine()) != null ) {
                String[] values = inputLine.split(",");

                long time = Long.parseLong(values[0]);
                double price = Double.parseDouble(values[1]);
                double amount = Double.parseDouble(values[2]);

                if ( amount == 0 )
                    continue;

                DataItem last = minutes.isEmpty() ? null : minutes.get(minutes.size()-1);
                long lastTime = 1000 * ( time - time % ONE_MINUTE.getLengthInSeconds() );
                if ( last == null || last.getTime() < lastTime ) {
                    minutes.add( new DataItem(lastTime, price, price, price, price, amount) );
                } else {
                    if (last.getHigh() < price) last.setHigh(price);
                    if (last.getLow() > price) last.setLow(price);
                    last.setClose(price);
                    last.setVolume(last.getVolume() + amount);
                }
            }
        }
        finally {
            rd.close();
        }

        for ( Interval i : SUPPORTED_INTERVALS ) {
            List<DataItem> data = aggregateTicks(minutes,i);

            synchronized ( (stock.toString() + "-" + i.getTimeParam()).intern() )
            {
                String fileName = getDatasetKey(stock,i);
                CacheManager.getInstance().cacheDataset(
                        new Dataset(data.subList(0,data.size()-1)),
                        fileName, true );
            }
        }
    }

    @Override
    public DataItem getLastDataItem(Stock stock, Interval interval)
    {
        List<DataItem> lastItems = getLastDataItems(stock, interval);
        if ( lastItems == null || lastItems.isEmpty() )
            return null;

        return lastItems.get(lastItems.size()-1);
    }

    @Override
    public List<DataItem> getLastDataItems(Stock stock, Interval interval)
    {
        synchronized ( (stock.toString() + "-" + ONE_MINUTE.getTimeParam()).intern() )
        {
            String minutesName = getDatasetKey(stock, ONE_MINUTE);
            if ( !DatasetUsage.getInstance().isDatasetInMemory(minutesName) )
                return new ArrayList<DataItem>(0);

            Dataset minutes = DatasetUsage.getInstance().getDatasetFromMemory(minutesName);

            if ( interval.equals(ONE_MINUTE) ) {
                List<DataItem> result = new ArrayList<DataItem>();
                result.add( minutes.getLastDataItem() );
                return result;
            }

            synchronized ( (stock.toString() + "-" + interval.getTimeParam()).intern() )
            {
                String dataName = getDatasetKey(stock, interval);
                Dataset data = DatasetUsage.getInstance().getDatasetFromMemory(dataName);

                int idx;
                for ( idx = minutes.getItemsCount(); idx > 0; idx-- ) {
                    if ( minutes.getDataItem(idx-1).getTime() < data.getLastTime() )
                        break;
                }

                List<DataItem> bars = aggregateTicks(
                        minutes.getDataItems().subList(idx, minutes.getItemsCount()),
                        interval );

                if ( bars.size() == 1 && bars.get(0).equals(data.getLastDataItem()) )
                    return new ArrayList<DataItem>(0);

                if ( !bars.isEmpty() ) {
                    data.getDataItems().set( data.getItemsCount()-1, bars.get(0) );

                    for ( DataItem bar : bars.subList(1, bars.size()) ) {
                        data.addDataItem(bar);
                    }
                }

                return bars;
            }
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
        return !bars.isEmpty();
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
        String s = symbolMap.get(symbol);
        return ( s == null ) ? null : "http://bitcoincharts.com/t/trades.csv?symbol=" + s;
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
        return 8002;
    }
}
