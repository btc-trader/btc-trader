package org.chartsy.main.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.chartsy.main.chartsy.ChartFrame;
import org.chartsy.main.chartsy.chart.AbstractOverlay;
import org.chartsy.main.chartsy.chart.Overlay;
import org.chartsy.main.managers.OverlayManager;
import org.openide.windows.WindowManager;

/**
 *
 * @author viorel.gheba
 */
public class AddOverlays extends javax.swing.JDialog {

    private ChartFrame parent;
    private Overlay[] initial;
    private Overlay[] selected;
    private AbstractOverlay[] unselected;
    private Vector<Object> selectedItems = new Vector<Object>();
    private Vector<Object> unselectedItems = new Vector<Object>();

    public AddOverlays(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        setIconImage(WindowManager.getDefault().getMainWindow().getIconImage());
        setTitle("Add Overlays");
    }
    public void setChartFrame(ChartFrame cf) { parent = cf; }

    public void initForm() {
        selected = new Overlay[0];
        unselected = new AbstractOverlay[0];

        unselected = OverlayManager.getDefault().getOverlays();
        selected = parent.getChartRenderer().getOverlays();
        initial = selected;

        setSelectedIndicators();
        setUnselectedIndicators();

        lblDescriptionValue.setText("");

        scrollPane.setLayout(new BorderLayout());
        scrollPane.setSize(324, 222);

        lstSelected.setListData(selectedItems);
        lstSelected.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstSelected.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                switch (e.getClickCount()) {
                    case 1:
                        btnAdd.setEnabled(false);
                        btnRemove.setEnabled(lstSelected.getLastVisibleIndex() != -1);
                        int index = lstSelected.locationToIndex(e.getPoint());
                        if (index != -1) {
                            Overlay ind = selected[index];
                            lblDescription.setText(ind.getDescription());
                            getPanel(ind);
                        }
                        break;
                    case 2:
                        btnRemove.doClick();
                        break;
                }
            }
        });

        lstUnselected.setListData(unselectedItems);
        lstUnselected.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstUnselected.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                switch (e.getClickCount()) {
                    case 1:
                        scrollPane.removeAll();
                        scrollPane.revalidate();
                        btnAdd.setEnabled(true);
                        btnRemove.setEnabled(false);
                        lblDescription.setText("");
                        break;
                    case 2:
                        btnAdd.doClick();
                        break;
                }
            }
        });

        btnAdd.setEnabled(false);
        btnAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = lstUnselected.getSelectedIndex();
                if (index != -1) {
                    AbstractOverlay ai = unselected[index];
                    Overlay[] list = new Overlay[selected.length + 1];
                    for (int j = 0; j < selected.length; j++) {
                        list[j] = selected[j];
                    }
                    list[selected.length] = ai.newInstance();
                    selected = list;
                    setSelectedIndicators();
                    lstSelected.setListData(selectedItems);
                }
            }
        });

        this.btnRemove.setEnabled(false);
        this.btnRemove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = lstSelected.getSelectedIndex();
                if (index != -1) {
                    if (selected.length == 1) {
                        Overlay[] list = new Overlay[0];
                        selected = list;
                    } else {
                        Overlay[] list = new Overlay[selected.length - 1];
                        int k = 0;
                        for (int j = 0; j < selected.length; j++) {
                            if (j != index) {
                                list[k] = selected[j];
                                k++;
                            }
                        }
                        selected = list;
                    }
                    setSelectedIndicators();
                    lstSelected.setListData(selectedItems);
                }
            }
        });

        this.btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selected = initial;
                setVisible(false);
            }
        });

        this.btnOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < selected.length; i++) {
                    selected[i].setDataset(parent.getChartRenderer().getMainDataset());
                    selected[i].calculate();
                }
                parent.getChartRenderer().setOverlays(selected);
                //parent.getChartRenderer().setOverlaysBounds();
                parent.getChartPanel().repaint();
                setVisible(false);
            }
        });

        this.btnApply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < selected.length; i++) {
                    selected[i].setDataset(parent.getChartRenderer().getMainDataset());
                    selected[i].calculate();
                }
                parent.getChartRenderer().setOverlays(selected);
                //parent.getChartRenderer().setOverlaysBounds();
                parent.getChartPanel().repaint();
            }
        });
    }

    private void getPanel(Overlay o) {
        PropertiesPanel p = new PropertiesPanel(o.getProperties());
        p.initComponents();
        p.setDialog(this);
        JScrollPane jsp = new JScrollPane(p);
        jsp.setPreferredSize(new Dimension(324, 222));
        scrollPane.removeAll();
        scrollPane.add(jsp, BorderLayout.CENTER);
        scrollPane.revalidate();
    }

    public void setSelectedIndicators() {
        selectedItems.clear();
        for (Overlay ind : selected) {
            selectedItems.add(ind.getLabel());
        }
    }

    public void setUnselectedIndicators() {
        unselectedItems.clear();
        for (AbstractOverlay ind : unselected) {
            unselectedItems.add(ind.getName());
        }
    }

    public void paint(Graphics g) {
        super.paint(g);
        setSelectedIndicators();
        lstSelected.setListData(selectedItems);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        lblIO = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstUnselected = new javax.swing.JList();
        lblSelected = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstSelected = new javax.swing.JList();
        btnAdd = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();
        lblProperties = new javax.swing.JLabel();
        lblDescription = new javax.swing.JLabel();
        lblDescriptionValue = new javax.swing.JLabel();
        btnCancel = new javax.swing.JButton();
        btnApply = new javax.swing.JButton();
        btnOk = new javax.swing.JButton();
        scrollPane = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        mainPanel.setBackground(new java.awt.Color(255, 255, 255));

        lblIO.setFont(new java.awt.Font("Tahoma", 1, 11));
        lblIO.setText(org.openide.util.NbBundle.getMessage(AddOverlays.class, "AddOverlays.lblIO.text")); // NOI18N

        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jScrollPane1.setViewportView(lstUnselected);

        lblSelected.setFont(new java.awt.Font("Tahoma", 1, 11));
        lblSelected.setText(org.openide.util.NbBundle.getMessage(AddOverlays.class, "AddOverlays.lblSelected.text")); // NOI18N

        jScrollPane2.setViewportView(lstSelected);

        btnAdd.setText(org.openide.util.NbBundle.getMessage(AddOverlays.class, "AddOverlays.btnAdd.text")); // NOI18N

        btnRemove.setText(org.openide.util.NbBundle.getMessage(AddOverlays.class, "AddOverlays.btnRemove.text")); // NOI18N

        lblProperties.setFont(new java.awt.Font("Tahoma", 1, 11));
        lblProperties.setText(org.openide.util.NbBundle.getMessage(AddOverlays.class, "AddOverlays.lblProperties.text")); // NOI18N

        lblDescription.setFont(new java.awt.Font("Tahoma", 1, 11));
        lblDescription.setText(org.openide.util.NbBundle.getMessage(AddOverlays.class, "AddOverlays.lblDescription.text")); // NOI18N

        lblDescriptionValue.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblDescriptionValue.setText(org.openide.util.NbBundle.getMessage(AddOverlays.class, "AddOverlays.lblDescriptionValue.text")); // NOI18N
        lblDescriptionValue.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        btnCancel.setText(org.openide.util.NbBundle.getMessage(AddOverlays.class, "AddOverlays.btnCancel.text")); // NOI18N

        btnApply.setText(org.openide.util.NbBundle.getMessage(AddOverlays.class, "AddOverlays.btnApply.text")); // NOI18N

        btnOk.setText(org.openide.util.NbBundle.getMessage(AddOverlays.class, "AddOverlays.btnOk.text")); // NOI18N

        scrollPane.setBackground(new java.awt.Color(255, 255, 255));
        scrollPane.setAutoscrolls(true);
        scrollPane.setMaximumSize(new java.awt.Dimension(324, 222));
        scrollPane.setMinimumSize(new java.awt.Dimension(324, 222));

        javax.swing.GroupLayout scrollPaneLayout = new javax.swing.GroupLayout(scrollPane);
        scrollPane.setLayout(scrollPaneLayout);
        scrollPaneLayout.setHorizontalGroup(
            scrollPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 413, Short.MAX_VALUE)
        );
        scrollPaneLayout.setVerticalGroup(
            scrollPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 222, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblIO, javax.swing.GroupLayout.DEFAULT_SIZE, 304, Short.MAX_VALUE)
                    .addComponent(lblSelected, javax.swing.GroupLayout.DEFAULT_SIZE, 304, Short.MAX_VALUE)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(btnAdd)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRemove)
                        .addGap(176, 176, 176))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(btnOk)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnApply)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel))
                    .addComponent(lblProperties, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
                    .addComponent(lblDescriptionValue, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
                    .addComponent(lblDescription)
                    .addComponent(scrollPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(lblProperties)
                        .addGap(11, 11, 11)
                        .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(11, 11, 11)
                        .addComponent(lblDescription)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblDescriptionValue, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCancel)
                            .addComponent(btnApply)
                            .addComponent(btnOk)))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(lblIO)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 129, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblSelected)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnAdd)
                            .addComponent(btnRemove))))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                AddOverlays dialog = new AddOverlays(new javax.swing.JFrame(), true);
                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                    public void windowClosing(java.awt.event.WindowEvent e) {
                        System.exit(0);
                    }
                });
                dialog.setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnApply;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOk;
    private javax.swing.JButton btnRemove;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblDescription;
    private javax.swing.JLabel lblDescriptionValue;
    private javax.swing.JLabel lblIO;
    private javax.swing.JLabel lblProperties;
    private javax.swing.JLabel lblSelected;
    private javax.swing.JList lstSelected;
    private javax.swing.JList lstUnselected;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel scrollPane;
    // End of variables declaration//GEN-END:variables

}