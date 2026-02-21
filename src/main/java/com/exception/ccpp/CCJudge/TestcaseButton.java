package com.exception.ccpp.CCJudge;

import com.exception.ccpp.CCJudge.Judge.JudgeVerdict;
import com.exception.ccpp.GUI.RoundedButton;
import com.exception.ccpp.GUI.RoundedPanel;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGException;
import com.kitfox.svg.SVGUniverse;
import kotlin.text.UStringsKt;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

public class TestcaseButton  extends RoundedButton {
    private final SVGLogo logo;
    private JLabel textLabel;
    private int testcaseNumber;
    public void setTestcaseNumber(int num)
    {
        testcaseNumber = num;
        textLabel.setText("Test case " + num);
    }
    public int getTestcaseNumber() {
        return  testcaseNumber;
    }

    public void setLogo(TestcaseLogo state)
    {
        SwingUtilities.invokeLater(()->{
            logo.setDiagram(getSVGPath(state));
            revalidate();
            repaint();
        });
    }


    public TestcaseButton(int testcaseNumber) {
        super("", 30);
        this.testcaseNumber = testcaseNumber;
        setBackground(Color.decode("#1a1c2a"));
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setBorder(BorderFactory.createEmptyBorder());
        setLayout(new GridBagLayout());


        GridBagConstraints gbcInternal = new GridBagConstraints();
        gbcInternal.gridx = 0;
        gbcInternal.gridy = 0;
        gbcInternal.weightx = 0.0;
        gbcInternal.fill = GridBagConstraints.BOTH;
        gbcInternal.anchor = GridBagConstraints.CENTER;

        // TODO@GLENSH INSERT TESTCASE LOGO/ICON HERE

        logo = new SVGLogo(null,new GridBagLayout(), 24, "#1a1c2a");
        logo.setBackground(Color.decode("#1a1c2a")); // default color
        logo.setBorderColor(Color.GRAY);
        logo.setBorderThickness(1);
        logo.setPreferredSize(new Dimension(24, 24));
        logo.setMinimumSize(new Dimension(24, 24));

        // Add the SVG canvas to the panel

        gbcInternal.insets = new Insets(10, 15, 10, 15);
        gbcInternal.anchor = GridBagConstraints.WEST;
        add(logo, gbcInternal);

        // --- 2. Text Label ---
        textLabel = new JLabel("Test case " + testcaseNumber);
        textLabel.setVerticalAlignment(SwingConstants.CENTER);
        textLabel.setForeground(Color.WHITE);
        textLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));

        gbcInternal.gridx = 1;
        gbcInternal.weightx = 1.0;
        gbcInternal.anchor = GridBagConstraints.CENTER;
        gbcInternal.insets = new Insets(10, 5, 10, 15);
        add(textLabel, gbcInternal);

        // Set size based on monitor (ADD THESE LINES)
        int monitorWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int monitorHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        int tempW = (int) (monitorWidth * 0.12);
        int tempH = (int) (monitorHeight * 0.05);
        setPreferredSize(new Dimension(tempW, tempH));

        setOpaque(false);
    }

    private String getSVGPath(TestcaseLogo verdict)
    {
        return switch (verdict) {
            case  null -> null;
            case  BRILLIANT -> "/verdicts/brilliant.svg";
            case  BEST -> "/verdicts/best.svg";
            case  INACCURACY -> "/verdicts/inaccuracy.svg";
            case  MISS -> "/verdicts/miss.svg";
            case  BLUNDER -> "/verdicts/blunder.svg";
            case  COMPILING -> "/verdicts/unnamed_redo.svg";
        };
    }

    public enum TestcaseLogo {
        BRILLIANT, // all are correct (AC)
        BEST, //most correct (AC, WA/WAE)
        INACCURACY, //only have excess (WAE)
        MISS, // has mismatch (WA)
        BLUNDER, // compiler error (RE,CE,TLE)
        COMPILING
    }

    public class SVGLogo extends RoundedPanel {
        private static ConcurrentHashMap<String, SVGDiagram> svgCache = new ConcurrentHashMap<>();

        SVGDiagram diagram;

        public SVGLogo(int radius) {
            super(radius);
        }

        public SVGLogo(String resPath,LayoutManager layout, int radius) {
            super(layout, radius);
            setOpaque(true);
            setDiagram(resPath);
        }

        public SVGLogo(String resPath, LayoutManager layout, int radius, String hexColor) {
            super(layout, radius, hexColor);
            setOpaque(true);
            setDiagram(resPath);
        }


        public void setDiagram(String resourcePath) {
            if (resourcePath == null) {
                diagram = null;
                return;
            };
            if (svgCache.containsKey(resourcePath)) {
                diagram = svgCache.get(resourcePath);
                return;
            }
            SVGUniverse universe = new SVGUniverse();
            URI uri = universe.loadSVG(getClass().getResource(resourcePath));
            diagram = universe.getDiagram(uri);
            svgCache.put(resourcePath, diagram);
        }
        private void refreshLogo()
        {
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (diagram == null) return;
            Graphics2D g2d = (Graphics2D) g.create();

            // scale the SVG to fit panel size
            float scaleX = getWidth() / (float) diagram.getWidth();
            float scaleY = getHeight() / (float) diagram.getHeight();
            g2d.scale(scaleX, scaleY);

            try {
                diagram.render(g2d);
            } catch (SVGException e) {}
            g2d.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            // optional: default size for layout managers
            return new Dimension(100, 100);
        }
    }




}