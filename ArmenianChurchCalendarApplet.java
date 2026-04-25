import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * ArmenianChurchCalendarApplet.java
 *
 * A legacy Java Applet / Swing desktop program for calculating the annual
 * Armenian Church Tonatsuyts values and selected feast dates for the
 * New/Gregorian calendar used in Armenia and most of the diaspora.
 *
 * Compile:
 *   javac ArmenianChurchCalendarApplet.java
 *
 * Run as a desktop Swing program:
 *   java ArmenianChurchCalendarApplet
 *
 * Note: Java applets are deprecated/removed in modern Java environments. The
 * main() method is included so this can be tested as a normal desktop app.
 */
@SuppressWarnings("serial")
public class ArmenianChurchCalendarApplet extends Applet {

    private JTextField yearField;
    private JLabel subtitleLabel;
    private JLabel taregirValue;
    private JLabel kirakagirValue;
    private JLabel veradirValue;
    private JLabel vosgegirValue;
    private JLabel easterValue;
    private JLabel leapYearValue;
    private JTable feastTable;
    private DefaultTableModel feastModel;
    private CalendarResult currentResult;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private static final Color BACKGROUND = new Color(245, 247, 250);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color HEADER_TEXT = new Color(35, 45, 60);
    private static final Color MUTED_TEXT = new Color(95, 105, 120);
    private static final Color ACCENT = new Color(49, 97, 150);
    private static final Color GRID = new Color(224, 229, 235);
    private static final Color INFO_BG = new Color(238, 244, 252);

    // Classical Armenian alphabet order. The 36th letter Ք is included because
    // leap-year Տարեգիր may need the next letter after Փ.
    private static final String[] ARMENIAN_LETTERS = {
        "Ա", "Բ", "Գ", "Դ", "Ե", "Զ", "Է", "Ը", "Թ",
        "Ժ", "Ի", "Լ", "Խ", "Ծ", "Կ", "Հ", "Ձ", "Ղ",
        "Ճ", "Մ", "Յ", "Ն", "Շ", "Ո", "Չ", "Պ", "Ջ",
        "Ռ", "Ս", "Վ", "Տ", "Ր", "Ց", "Ւ", "Փ", "Ք"
    };

    private static final String[] WEEK_LETTERS = {"Ա", "Բ", "Գ", "Դ", "Ե", "Զ", "Է"};

    @Override
    public void init() {
        installLookAndFeel();
        buildUi();
        calculateAndDisplay();
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to default Swing look and feel.
        }
    }

    private void buildUi() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        setFont(new Font("Dialog", Font.PLAIN, 14));

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BACKGROUND);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        root.add(buildHeaderPanel(), BorderLayout.NORTH);
        root.add(buildContentPanel(), BorderLayout.CENTER);

        add(root, BorderLayout.CENTER);
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(12, 10));
        header.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Armenian Church Calendar Calculator");
        title.setFont(new Font("Dialog", Font.BOLD, 24));
        title.setForeground(HEADER_TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitleLabel = new JLabel("New/Gregorian Armenian Church calendar");
        subtitleLabel.setFont(new Font("Dialog", Font.PLAIN, 13));
        subtitleLabel.setForeground(MUTED_TEXT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBlock.add(title);
        titleBlock.add(subtitleLabel);
        header.add(titleBlock, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridBagLayout());
        controls.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 4, 0, 4);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel yearLabel = new JLabel("Year");
        yearLabel.setFont(new Font("Dialog", Font.BOLD, 13));
        yearLabel.setForeground(HEADER_TEXT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        controls.add(yearLabel, gbc);

        yearField = new JTextField("2026", 8);
        yearField.setFont(new Font("Dialog", Font.PLAIN, 15));
        yearField.setHorizontalAlignment(SwingConstants.CENTER);
        yearField.setPreferredSize(new Dimension(95, 32));
        gbc.gridx = 1;
        controls.add(yearField, gbc);

        JButton calculateButton = new JButton("Calculate");
        calculateButton.setFont(new Font("Dialog", Font.BOLD, 13));
        calculateButton.setPreferredSize(new Dimension(110, 32));
        gbc.gridx = 2;
        controls.add(calculateButton, gbc);

        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateAndDisplay();
            }
        });
        yearField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateAndDisplay();
            }
        });

        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private JPanel buildContentPanel() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);

        content.add(buildSummaryCard(), BorderLayout.NORTH);
        content.add(buildFeastCard(), BorderLayout.CENTER);
        content.add(buildBottomNotesCard(), BorderLayout.SOUTH);

        return content;
    }

    private JPanel buildSummaryCard() {
        JPanel card = createCardPanel(new BorderLayout(0, 12));
        card.add(sectionLabel("Annual Tonatsuyts Values"), BorderLayout.NORTH);

        JPanel values = new JPanel(new GridBagLayout());
        values.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        taregirValue = addValueTile(values, gbc, 0, 0, "Տարեգիր / Գիր տարւոյ", "Տարեգիր / Գիր տարւոյ");
        kirakagirValue = addValueTile(values, gbc, 1, 0, "Կիրակագիր / Եօթներեակ", "Կիրակագիր / Եօթներեակ");
        veradirValue = addValueTile(values, gbc, 2, 0, "Վերադիր", "Վերադիր");
        vosgegirValue = addValueTile(values, gbc, 3, 0, "Ոսկեգիր / Իննեւտասներեակ", "Ոսկեգիր / Իննեւտասներեակ");
        easterValue = addValueTile(values, gbc, 0, 1, "Gregorian Easter", "Gregorian Easter");
        leapYearValue = addValueTile(values, gbc, 1, 1, "Leap Year", "Leap Year");

        card.add(values, BorderLayout.CENTER);
        return card;
    }

    private JLabel addValueTile(JPanel parent, GridBagConstraints gbc, int x, int y,
                                final String label, final String noteKey) {
        JPanel tile = new JPanel(new BorderLayout(4, 5));
        tile.setBackground(new Color(250, 252, 255));
        tile.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRID),
            new EmptyBorder(10, 12, 10, 12)
        ));

        JPanel labelLine = new JPanel(new BorderLayout(4, 0));
        labelLine.setOpaque(false);

        JLabel labelView = new JLabel(label);
        labelView.setFont(new Font("Dialog", Font.PLAIN, 12));
        labelView.setForeground(MUTED_TEXT);
        labelLine.add(labelView, BorderLayout.CENTER);

        JButton info = createInfoButton();
        info.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showInfoDialog(noteKey, buildAnnualValueNote(noteKey));
            }
        });
        labelLine.add(info, BorderLayout.EAST);

        JLabel value = new JLabel("—");
        value.setFont(new Font("Dialog", Font.BOLD, 20));
        value.setForeground(HEADER_TEXT);

        tile.add(labelLine, BorderLayout.NORTH);
        tile.add(value, BorderLayout.CENTER);

        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 1.0;
        parent.add(tile, gbc);
        return value;
    }

    private JPanel buildFeastCard() {
        JPanel card = createCardPanel(new BorderLayout(0, 12));
        card.add(sectionLabel("Selected Feast Dates"), BorderLayout.NORTH);

        feastModel = new DefaultTableModel(new Object[] {"Feast", "Date", "Day", "Info"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
        };

        feastTable = new JTable(feastModel);
        feastTable.setRowHeight(30);
        feastTable.setShowGrid(false);
        feastTable.setIntercellSpacing(new Dimension(0, 0));
        feastTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        feastTable.setFont(new Font("Dialog", Font.PLAIN, 13));
        feastTable.setForeground(HEADER_TEXT);
        feastTable.setFillsViewportHeight(true);

        JTableHeader header = feastTable.getTableHeader();
        header.setFont(new Font("Dialog", Font.BOLD, 12));
        header.setForeground(HEADER_TEXT);
        header.setBackground(new Color(232, 237, 244));
        header.setReorderingAllowed(false);

        feastTable.getColumnModel().getColumn(0).setPreferredWidth(345);
        feastTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        feastTable.getColumnModel().getColumn(2).setPreferredWidth(95);
        feastTable.getColumnModel().getColumn(3).setPreferredWidth(55);
        feastTable.getColumnModel().getColumn(3).setMaxWidth(70);

        feastTable.setDefaultRenderer(Object.class, new FeastTableRenderer());
        feastTable.getColumnModel().getColumn(3).setCellRenderer(new InfoButtonCell());
        feastTable.getColumnModel().getColumn(3).setCellEditor(new InfoButtonCell());

        JScrollPane scroll = new JScrollPane(feastTable);
        scroll.setBorder(BorderFactory.createLineBorder(GRID));
        scroll.getViewport().setBackground(CARD_BACKGROUND);
        scroll.setPreferredSize(new Dimension(900, 520));

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBottomNotesCard() {
        JPanel card = createCardPanel(new BorderLayout(0, 8));
        card.add(sectionLabel("Notes"), BorderLayout.NORTH);

        JTextArea notes = new JTextArea();
        notes.setEditable(false);
        notes.setOpaque(false);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.setFont(new Font("Dialog", Font.PLAIN, 12));
        notes.setForeground(MUTED_TEXT);
        notes.setText(
            "This program uses the New/Gregorian Armenian Church calendar used in Armenia and most of the diaspora. "
            + "It does not calculate the Old/Julian Jerusalem calendar. Click the circled information button beside any value or feast for the calculation rule. "
            + "Dominical feasts are marked with †. "
            + "Please send any feedback regarding the accuracy or functionality of this applet to Fr. Haroutiun Sabounjian at frharoutiun@gmail.com."
        );
        card.add(notes, BorderLayout.CENTER);
        return card;
    }

    private JPanel createCardPanel(java.awt.LayoutManager layout) {
        JPanel card = new JPanel(layout);
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRID),
            new EmptyBorder(14, 14, 14, 14)
        ));
        return card;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Dialog", Font.BOLD, 16));
        label.setForeground(HEADER_TEXT);
        return label;
    }

    private JButton createInfoButton() {
        JButton b = new JButton("ⓘ");
        b.setFont(new Font("Dialog", Font.BOLD, 13));
        b.setForeground(ACCENT);
        b.setBackground(INFO_BG);
        b.setBorder(BorderFactory.createLineBorder(new Color(190, 210, 235)));
        b.setFocusPainted(false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(24, 24));
        b.setToolTipText("Show calculation note");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void calculateAndDisplay() {
        String text = yearField.getText().trim();
        int year;
        try {
            year = Integer.parseInt(text);
            if (year < 1583 || year > 4099) {
                JOptionPane.showMessageDialog(this,
                    "Please enter a Gregorian year from 1583 to 4099.",
                    "Invalid year",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid year, for example: 2026.",
                "Invalid year",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentResult = calculateCalendar(year);
        updateSummary(currentResult);
        updateFeastTable(currentResult);
    }

    private void updateSummary(CalendarResult r) {
        subtitleLabel.setText("New/Gregorian Armenian Church calendar · Year " + r.year);
        taregirValue.setText(r.taregir);
        kirakagirValue.setText(r.kirakagir);
        veradirValue.setText(String.valueOf(r.veradir));
        vosgegirValue.setText(String.valueOf(r.vosgegir));
        easterValue.setText(r.easter.format(SHORT_DATE_FMT));
        leapYearValue.setText(r.isLeapYear ? "Yes" : "No");
    }

    private void updateFeastTable(CalendarResult r) {
        feastModel.setRowCount(0);
        for (Map.Entry<String, LocalDate> entry : r.feasts.entrySet()) {
            LocalDate date = entry.getValue();
            feastModel.addRow(new Object[] {
                entry.getKey(),
                date.format(SHORT_DATE_FMT),
                capitalize(date.getDayOfWeek().toString()),
                "ⓘ"
            });
        }
    }

    private void showInfoForFeastRow(int row) {
        if (currentResult == null || row < 0 || row >= feastModel.getRowCount()) {
            return;
        }
        String feast = String.valueOf(feastModel.getValueAt(row, 0));
        showInfoDialog(feast, buildFeastNote(feast, currentResult));
    }

    private void showInfoDialog(String title, String message) {
        JTextArea area = new JTextArea(message, 12, 52);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Dialog", Font.PLAIN, 13));
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        area.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(560, 270));
        JOptionPane.showMessageDialog(this, scroll, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private String buildAnnualValueNote(String key) {
        if (currentResult == null) {
            return "Calculate a year first.";
        }
        int year = currentResult.year;
        if ("Տարեգիր / Գիր տարւոյ".equals(key)) {
            LocalDate march22 = LocalDate.of(year, 3, 22);
            int number = (int) (currentResult.easter.toEpochDay() - march22.toEpochDay()) + 1;
            return "Տարեգիր / Գիր տարւոյ is found from Gregorian Easter. Count inclusively from March 22: March 22 = Ա, March 23 = Բ, and so on.\n\n"
                + "For " + year + ", Easter is " + currentResult.easter.format(DATE_FMT) + ". That is position " + number + " in the March 22-April 25 range.\n\n"
                + "The resulting Easter-letter is " + ARMENIAN_LETTERS[number - 1] + ". "
                + (currentResult.isLeapYear
                    ? "Because this is a leap year, the printed Տարեգիր has two letters: the next letter first, followed by the Easter-letter. Result: " + currentResult.taregir + "."
                    : "Because this is not a leap year, the result is a single letter: " + currentResult.taregir + ".");
        }
        if ("Կիրակագիր / Եօթներեակ".equals(key)) {
            return "Կիրակագիր / Եօթներեակ is the Sunday-letter. Assign January 1 = Ա, January 2 = Բ, January 3 = Գ, through January 7 = Է, then repeat. The letter that falls on Sunday is the Կիրակագիր.\n\n"
                + "For leap years there are two letters: one before February 29 and one after February 29, because the leap day shifts the weekly letter alignment.\n\n"
                + "For " + year + ", the result is " + currentResult.kirakagir + ".";
        }
        if ("Վերադիր".equals(key)) {
            return "Վերադիր is a Gregorian epact-type lunar correction used with the 19-year cycle in the Paschal calculation.\n\n"
                + "Formula used here:\n"
                + "a = year mod 19\n"
                + "b = floor(year / 100)\n"
                + "d = floor(b / 4)\n"
                + "f = floor((b + 8) / 25)\n"
                + "g = floor((b - f + 1) / 3)\n"
                + "Վերադիր = (11a + 18 + b - d - g) mod 30; if the result is 0, write 30.\n\n"
                + "For " + year + ", the result is " + currentResult.veradir + ".";
        }
        if ("Ոսկեգիր / Իննեւտասներեակ".equals(key)) {
            return "Ոսկեգիր / Իննեւտասներեակ is the year's place in the 19-year lunar cycle.\n\n"
                + "Formula: Ոսկեգիր = (Gregorian year mod 19) + 1. If the remainder gives 0 in older notation, the cycle value is 19.\n\n"
                + "For " + year + ", the result is " + currentResult.vosgegir + ".";
        }
        if ("Gregorian Easter".equals(key)) {
            return "Gregorian Easter is calculated by the standard Gregorian computus, here using the Meeus/Jones/Butcher algorithm. This Easter date is then used to construct the Paschal cycle and to find the Տարեգիր.\n\n"
                + "For " + year + ", Easter is " + currentResult.easter.format(DATE_FMT) + ".";
        }
        if ("Leap Year".equals(key)) {
            return "Gregorian leap year rule: a year is a leap year if it is divisible by 4, except century years must also be divisible by 400.\n\n"
                + "Leap years affect the two-letter forms of Տարեգիր and Կիրակագիր.\n\n"
                + year + " is " + (currentResult.isLeapYear ? "a leap year." : "not a leap year.");
        }
        return "No note available.";
    }

    private String buildFeastNote(String feast, CalendarResult r) {
        LocalDate easter = r.easter;
        LocalDate poon = easter.minusDays(49);
        LocalDate greatLent = easter.minusDays(48);
        LocalDate pentecost = easter.plusDays(49);
        LocalDate holyEtchmiadzin = pentecost.plusDays(14);
        LocalDate exaltation = sundayNearest(LocalDate.of(r.year, 9, 14));
        LocalDate hisnagParegentan = sundayNearest(LocalDate.of(r.year, 11, 18));
        LocalDate crossSunday8 = exaltation.plusWeeks(7);
        LocalDate crossSunday10 = exaltation.plusWeeks(9);
        LocalDate crossSunday11 = exaltation.plusWeeks(10);

        if (feast.startsWith("Nativity and Theophany")) {
            return "Nativity and Theophany of Our Lord is fixed on January 6 in the Armenian Church calendar.\n\nFor " + r.year + ": January 6 = " + LocalDate.of(r.year, 1, 6).format(DATE_FMT) + ".";
        }
        if (feast.startsWith("Presentation of the Lord")) {
            return "Presentation of the Lord to the Temple is fixed on February 14, forty days after January 6.\n\nFor " + r.year + ": February 14 = " + LocalDate.of(r.year, 2, 14).format(DATE_FMT) + ".";
        }
        if (feast.startsWith("St. Sarkis")) {
            return "St. Sarkis is placed on the Saturday before the Fast of the Catechumens, two weeks before Poon Paregentan. In this program it is calculated as Poon Paregentan minus 15 days.\n\nPoon Paregentan: " + poon.format(DATE_FMT) + "\nSt. Sarkis: " + poon.minusDays(15).format(DATE_FMT) + ".";
        }
        if (feast.startsWith("St. Ghevont")) {
            return "St. Ghevont the Priest and His Companions is calculated here as the Tuesday before Poon Paregentan: Poon Paregentan minus 5 days.\n\nPoon Paregentan: " + poon.format(DATE_FMT) + "\nSt. Ghevont: " + poon.minusDays(5).format(DATE_FMT) + ".";
        }
        if (feast.startsWith("St. Vartan")) {
            return "St. Vartan the Warrior and His Companions is calculated here as the Thursday before Poon Paregentan: Poon Paregentan minus 3 days.\n\nPoon Paregentan: " + poon.format(DATE_FMT) + "\nSt. Vartan: " + poon.minusDays(3).format(DATE_FMT) + ".";
        }
        if (feast.equals("Poon Paregentan")) {
            return "Poon Paregentan is the Sunday immediately before the beginning of Great Lent. It is 7 Sundays before Easter, or Easter minus 49 days.\n\nEaster: " + easter.format(DATE_FMT) + "\nPoon Paregentan: " + poon.format(DATE_FMT) + ".";
        }
        if (feast.equals("Great Lent begins")) {
            return "Great Lent begins on the Monday after Poon Paregentan, calculated as Easter minus 48 days.\n\nEaster: " + easter.format(DATE_FMT) + "\nGreat Lent begins: " + greatLent.format(DATE_FMT) + ".";
        }
        if (feast.startsWith("Meejeenk")) {
            return "Meejeenk / Միջինք is the Wednesday of the fourth week of Great Lent. In this program it is calculated as Great Lent begins plus 23 days, which places it on Wednesday.\n\nGreat Lent begins: " + greatLent.format(DATE_FMT) + "\nMeejeenk: " + greatLent.plusDays(23).format(DATE_FMT) + ".";
        }
        if (feast.equals("Annunciation")) {
            return "Annunciation is fixed on April 7.\n\nFor " + r.year + ": April 7 = " + LocalDate.of(r.year, 4, 7).format(DATE_FMT) + ".";
        }
        if (feast.equals("Palm Sunday")) {
            return "Palm Sunday is the Sunday before Easter, calculated as Easter minus 7 days.\n\nEaster: " + easter.format(DATE_FMT) + "\nPalm Sunday: " + easter.minusDays(7).format(DATE_FMT) + ".";
        }
        if (feast.startsWith("Easter")) {
            return "Easter / Resurrection of Our Lord is calculated by the Gregorian computus for the New/Gregorian Armenian Church calendar. The program uses the Meeus/Jones/Butcher algorithm.\n\nFor " + r.year + ", Easter is " + easter.format(DATE_FMT) + ".";
        }
        if (feast.equals("Ascension")) {
            return "Ascension is the 40th day of Easter, counting Easter Sunday as day 1. In date arithmetic, it is Easter plus 39 days.\n\nEaster: " + easter.format(DATE_FMT) + "\nAscension: " + easter.plusDays(39).format(DATE_FMT) + ".";
        }
        if (feast.equals("Pentecost")) {
            return "Pentecost is the 50th day of Easter, counting Easter Sunday as day 1. In date arithmetic, it is Easter plus 49 days.\n\nEaster: " + easter.format(DATE_FMT) + "\nPentecost: " + pentecost.format(DATE_FMT) + ".";
        }
        if (feast.startsWith("Emergence of St. Gregory")) {
            return "The Emergence of St. Gregory from the Pit is the Saturday immediately before the Feast of Holy Etchmiadzin.\n\nHoly Etchmiadzin: " + holyEtchmiadzin.format(DATE_FMT) + "\nEmergence: " + holyEtchmiadzin.minusDays(1).format(DATE_FMT) + ".";
        }
        if (feast.equals("Holy Etchmiadzin")) {
            return "Holy Etchmiadzin is calculated as the second Sunday after Pentecost, or Pentecost plus 14 days.\n\nPentecost: " + pentecost.format(DATE_FMT) + "\nHoly Etchmiadzin: " + holyEtchmiadzin.format(DATE_FMT) + ".";
        }
        if (feast.startsWith("Transfiguration")) {
            return "Transfiguration / Vardavar is calculated here as Easter plus 98 days, the 14th Sunday after Easter.\n\nEaster: " + easter.format(DATE_FMT) + "\nTransfiguration: " + easter.plusDays(98).format(DATE_FMT) + ".";
        }
        if (feast.startsWith("Assumption")) {
            return "Assumption of the Holy Mother-of-God is kept on the Sunday nearest August 15.\n\nFor " + r.year + ", the Sunday nearest August 15 is " + sundayNearest(LocalDate.of(r.year, 8, 15)).format(DATE_FMT) + ".";
        }
        if (feast.startsWith("Nativity of the Holy Mother")) {
            return "Nativity of the Holy Mother-of-God is fixed on September 8.\n\nFor " + r.year + ": September 8 = " + LocalDate.of(r.year, 9, 8).format(DATE_FMT) + ".";
        }
        if (feast.startsWith("Exaltation")) {
            return "Exaltation of the Holy Cross is the Sunday nearest September 14. This Sunday is counted as the 1st Sunday of the Holy Cross season.\n\nFor " + r.year + ", the Sunday nearest September 14 is " + exaltation.format(DATE_FMT) + ".";
        }
        if (feast.equals("Holy Cross of Varak")) {
            return "Holy Cross of Varak is calculated as the second Sunday after Exaltation of the Holy Cross, or Exaltation plus 14 days.\n\nExaltation: " + exaltation.format(DATE_FMT) + "\nHoly Cross of Varak: " + exaltation.plusDays(14).format(DATE_FMT) + ".";
        }
        if (feast.equals("Holy Translators")) {
            return "Holy Translators is calculated here as the Saturday before the 5th Sunday of the Holy Cross season, or Exaltation plus 27 days.\n\nExaltation: " + exaltation.format(DATE_FMT) + "\nHoly Translators: " + exaltation.plusDays(27).format(DATE_FMT) + ".";
        }
        if (feast.equals("Discovery of the Holy Cross")) {
            return "Discovery of the Holy Cross is calculated here as the Sunday on or after October 23.\n\nFor " + r.year + ": " + sundayOnOrAfter(LocalDate.of(r.year, 10, 23)).format(DATE_FMT) + ".";
        }
        if (feast.equals("All Saints Day")) {
            String caseText;
            if (hisnagParegentan.equals(crossSunday10)) {
                caseText = "Hisnag Paregentan falls on the 10th Sunday of the Holy Cross, so this is the shorter Cross season. All Saints is kept on the Saturday before the 8th Sunday of the Holy Cross.";
            } else if (hisnagParegentan.equals(crossSunday11)) {
                caseText = "Hisnag Paregentan falls on the 11th Sunday of the Holy Cross, so this is the longer Cross season with an added week. All Saints is moved to the Saturday before the 11th Sunday / Hisnag Paregentan.";
            } else {
                caseText = "Hisnag Paregentan did not match the expected 10th or 11th Sunday of the Holy Cross; check the year calculation.";
            }
            return "All Saints follows the two-case rule given in the Tonatsuyts notes, not a single fixed distance from Hisnag Paregentan.\n\n"
                + "Exaltation / 1st Sunday of the Cross: " + exaltation.format(DATE_FMT) + "\n"
                + "8th Sunday of the Cross: " + crossSunday8.format(DATE_FMT) + "\n"
                + "10th Sunday of the Cross: " + crossSunday10.format(DATE_FMT) + "\n"
                + "11th Sunday of the Cross: " + crossSunday11.format(DATE_FMT) + "\n"
                + "Hisnag Paregentan: " + hisnagParegentan.format(DATE_FMT) + "\n\n"
                + caseText + "\n\n"
                + "All Saints: " + calculateAllSaintsFromTonatsuytsRule(exaltation, hisnagParegentan).format(DATE_FMT) + ".";
        }
        if (feast.equals("Archangels Gabriel and Michael")) {
            return "Archangels Gabriel and Michael is always the Saturday after the 8th Sunday of the Holy Cross.\n\n"
                + "Exaltation / 1st Sunday of the Cross: " + exaltation.format(DATE_FMT) + "\n"
                + "8th Sunday of the Cross: " + crossSunday8.format(DATE_FMT) + "\n"
                + "Saturday after the 8th Sunday: " + crossSunday8.plusDays(6).format(DATE_FMT) + ".";
        }
        if (feast.equals("Presentation of the Holy Mother-of-God")) {
            return "Presentation of the Holy Mother-of-God is fixed on November 21.\n\nFor " + r.year + ": November 21 = " + LocalDate.of(r.year, 11, 21).format(DATE_FMT) + ".";
        }
        if (feast.equals("Conception of the Holy Mother-of-God")) {
            return "Conception of the Holy Mother-of-God is fixed on December 9.\n\nFor " + r.year + ": December 9 = " + LocalDate.of(r.year, 12, 9).format(DATE_FMT) + ".";
        }
        if (feast.equals("St. James of Nisibis")) {
            return "St. James of Nisibis is calculated here as the Saturday on or after December 12.\n\nFor " + r.year + ": " + saturdayOnOrAfter(LocalDate.of(r.year, 12, 12)).format(DATE_FMT) + ".";
        }
        return "No calculation note is available for this item.";
    }

    private static CalendarResult calculateCalendar(int year) {
        CalendarResult r = new CalendarResult();
        r.year = year;
        r.easter = gregorianEaster(year);
        r.isLeapYear = isGregorianLeapYear(year);

        r.vosgegir = calculateVosgegir(year);
        r.veradir = calculateVeradir(year);
        r.kirakagir = calculateKirakagir(year);
        r.taregir = calculateTaregir(r.easter, r.isLeapYear);

        r.feasts = calculateFeasts(year, r.easter);
        return r;
    }

    /** Gregorian Easter calculation, Meeus/Jones/Butcher algorithm. */
    private static LocalDate gregorianEaster(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }

    private static boolean isGregorianLeapYear(int year) {
        return (year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0));
    }

    /** Ոսկեգիր / Իննեւտասներեակ: Gregorian golden number, range 1-19. */
    private static int calculateVosgegir(int year) {
        return (year % 19) + 1;
    }

    /** Վերադիր, calculated as the Gregorian epact-type correction. */
    private static int calculateVeradir(int year) {
        int a = year % 19;
        int b = year / 100;
        int d = b / 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int value = mod(11 * a + 18 + b - d - g, 30);
        return value == 0 ? 30 : value;
    }

    /** Կիրակագիր / Եօթներեակ. */
    private static String calculateKirakagir(int year) {
        LocalDate jan1 = LocalDate.of(year, 1, 1);
        int daysUntilSunday = DayOfWeek.SUNDAY.getValue() - jan1.getDayOfWeek().getValue();
        if (daysUntilSunday < 0) {
            daysUntilSunday += 7;
        }
        LocalDate firstSunday = jan1.plusDays(daysUntilSunday);
        int firstIndex = (firstSunday.getDayOfYear() - 1) % 7;
        String first = WEEK_LETTERS[firstIndex];

        if (!isGregorianLeapYear(year)) {
            return first;
        }

        int secondIndex = mod(firstIndex - 1, 7);
        return first + WEEK_LETTERS[secondIndex];
    }

    /** Տարեգիր / Գիր տարւոյ. */
    private static String calculateTaregir(LocalDate easter, boolean leapYear) {
        LocalDate march22 = LocalDate.of(easter.getYear(), 3, 22);
        int index = (int) (easter.toEpochDay() - march22.toEpochDay()); // 0-based
        if (index < 0 || index >= 35) {
            throw new IllegalStateException("Easter date outside expected Gregorian range.");
        }

        String easterLetter = ARMENIAN_LETTERS[index];
        if (!leapYear) {
            return easterLetter;
        }

        String leapLetter = ARMENIAN_LETTERS[index + 1];
        return leapLetter + easterLetter;
    }

    private static Map<String, LocalDate> calculateFeasts(int year, LocalDate easter) {
        Map<String, LocalDate> feasts = new LinkedHashMap<String, LocalDate>();

        LocalDate poonParegentan = easter.minusDays(49);
        LocalDate greatLentBegins = easter.minusDays(48);
        LocalDate palmSunday = easter.minusDays(7);
        LocalDate pentecost = easter.plusDays(49);
        LocalDate holyEtchmiadzin = pentecost.plusDays(14);
        LocalDate exaltation = sundayNearest(LocalDate.of(year, 9, 14));
        LocalDate hisnagParegentan = sundayNearest(LocalDate.of(year, 11, 18));
        LocalDate allSaints = calculateAllSaintsFromTonatsuytsRule(exaltation, hisnagParegentan);
        LocalDate crossSunday8 = exaltation.plusWeeks(7);

        feasts.put("Nativity and Theophany of Our Lord †", LocalDate.of(year, 1, 6));
        feasts.put("Presentation of the Lord to the Temple", LocalDate.of(year, 2, 14));
        feasts.put("St. Sarkis the Warrior", poonParegentan.minusDays(15));
        feasts.put("St. Ghevont the Priest and His Companions", poonParegentan.minusDays(5));
        feasts.put("St. Vartan the Warrior and His Companions", poonParegentan.minusDays(3));
        feasts.put("Poon Paregentan", poonParegentan);
        feasts.put("Great Lent begins", greatLentBegins);
        feasts.put("Meejeenk (Median day of Great Lent)", greatLentBegins.plusDays(23));
        feasts.put("Annunciation", LocalDate.of(year, 4, 7));
        feasts.put("Palm Sunday", palmSunday);
        feasts.put("Easter - Resurrection of Our Lord †", easter);
        feasts.put("Ascension", easter.plusDays(39));
        feasts.put("Pentecost", pentecost);
        feasts.put("Emergence of St. Gregory from the Pit", holyEtchmiadzin.minusDays(1));
        feasts.put("Holy Etchmiadzin", holyEtchmiadzin);
        feasts.put("Transfiguration of Our Lord †", easter.plusDays(98));
        feasts.put("Assumption of the Holy Mother-of-God †", sundayNearest(LocalDate.of(year, 8, 15)));
        feasts.put("Nativity of the Holy Mother-of-God", LocalDate.of(year, 9, 8));
        feasts.put("Exaltation of the Holy Cross †", exaltation);
        feasts.put("Holy Cross of Varak", exaltation.plusDays(14));
        feasts.put("Holy Translators", exaltation.plusDays(27));
        feasts.put("Discovery of the Holy Cross", sundayOnOrAfter(LocalDate.of(year, 10, 23)));
        feasts.put("All Saints Day", allSaints);
        feasts.put("Archangels Gabriel and Michael", crossSunday8.plusDays(6));
        feasts.put("Presentation of the Holy Mother-of-God", LocalDate.of(year, 11, 21));
        feasts.put("Conception of the Holy Mother-of-God", LocalDate.of(year, 12, 9));
        feasts.put("St. James of Nisibis", saturdayOnOrAfter(LocalDate.of(year, 12, 12)));

        return feasts;
    }

    /**
     * All Saints Day according to the two Tonatsuyts notes:
     * - If Hisnag Paregentan is the 10th Sunday of the Cross, All Saints is
     *   the Saturday before the 8th Sunday of the Cross.
     * - If Hisnag Paregentan is the 11th Sunday of the Cross, All Saints is
     *   the Saturday before that 11th Sunday / Hisnag Paregentan.
     */
    private static LocalDate calculateAllSaintsFromTonatsuytsRule(LocalDate exaltation,
                                                                  LocalDate hisnagParegentan) {
        LocalDate crossSunday8 = exaltation.plusWeeks(7);
        LocalDate crossSunday10 = exaltation.plusWeeks(9);
        LocalDate crossSunday11 = exaltation.plusWeeks(10);

        if (hisnagParegentan.equals(crossSunday10)) {
            return crossSunday8.minusDays(1);
        }

        if (hisnagParegentan.equals(crossSunday11)) {
            return crossSunday11.minusDays(1);
        }

        throw new IllegalStateException(
            "Hisnag Paregentan did not fall on the 10th or 11th Sunday of the Cross."
        );
    }

    /** Sunday nearest to the supplied date. If equally close, this chooses the following Sunday. */
    private static LocalDate sundayNearest(LocalDate date) {
        LocalDate before = date;
        while (before.getDayOfWeek() != DayOfWeek.SUNDAY) {
            before = before.minusDays(1);
        }

        LocalDate after = date;
        while (after.getDayOfWeek() != DayOfWeek.SUNDAY) {
            after = after.plusDays(1);
        }

        long daysBefore = date.toEpochDay() - before.toEpochDay();
        long daysAfter = after.toEpochDay() - date.toEpochDay();
        return daysBefore < daysAfter ? before : after;
    }

    private static LocalDate sundayOnOrAfter(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private static LocalDate saturdayOnOrAfter(LocalDate date) {
        while (date.getDayOfWeek() != DayOfWeek.SATURDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private static int mod(int value, int divisor) {
        int m = value % divisor;
        return m < 0 ? m + divisor : m;
    }

    private static String capitalize(String text) {
        if (text == null || text.length() == 0) {
            return text;
        }
        String lower = text.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static class CalendarResult {
        int year;
        boolean isLeapYear;
        LocalDate easter;
        String taregir;
        String kirakagir;
        int veradir;
        int vosgegir;
        Map<String, LocalDate> feasts;
    }

    private class FeastTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
            }
            c.setForeground(HEADER_TEXT);
            setBorder(new EmptyBorder(0, 8, 0, 8));

            String feast = String.valueOf(table.getValueAt(row, 0));
            if (feast.contains("†")) {
                c.setFont(new Font("Dialog", Font.BOLD, 13));
            } else {
                c.setFont(new Font("Dialog", Font.PLAIN, 13));
            }

            if (column == 3) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            return c;
        }
    }

    private class InfoButtonCell extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
        private final JButton button;
        private int row = -1;

        InfoButtonCell() {
            button = createInfoButton();
            button.addActionListener(this);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 3));
            panel.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253)));
            panel.add(createInfoButton());
            return panel;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 3));
            panel.setBackground(table.getSelectionBackground());
            panel.add(button);
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "ⓘ";
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int modelRow = feastTable.convertRowIndexToModel(row);
            fireEditingStopped();
            showInfoForFeastRow(modelRow);
        }
    }

    /** Standalone Swing runner for environments where applets are not supported. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Armenian Church Calendar Calculator");
                ArmenianChurchCalendarApplet applet = new ArmenianChurchCalendarApplet();
                applet.init();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.getContentPane().add(applet);
                frame.setMinimumSize(new Dimension(980, 760));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
