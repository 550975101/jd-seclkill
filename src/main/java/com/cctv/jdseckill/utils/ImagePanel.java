package com.cctv.jdseckill.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImagePanel extends JPanel {

    BufferedImage image;

    public ImagePanel(Image image) {
        // Not really need a BufferedImage, just a requirement
        this.image = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR);
        Graphics g = this.image.getGraphics();
        g.drawImage(image, getHeight(), getWidth(), null);
    }

    @Override
    public void paintComponent(Graphics g) {
        g.drawImage(image,160, 160, null);
    }
}
