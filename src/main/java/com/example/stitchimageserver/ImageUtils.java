package com.example.stitchimageserver;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class ImageUtils {

    /**
     * 裁剪原图，生成图片锁（大图）和图片钥匙（小图）
     *
     * @param srcImageName    原图名称
     * @param imageDir   裁剪后的图片目录路径
     * @param lock 图片锁名称
     * @param key 图片钥匙名称
     * @param x          裁剪开始x坐标
     * @param y          裁剪开始y坐标
     * @param width      裁剪宽度
     * @param height     裁剪高度
     * @param formatName 图片类型
     */
    public static boolean cutImage(String srcImageName, String imageDir, String lock, String key, int x, int y, int width, int height, String formatName) throws IOException {
        Resource resource = new ClassPathResource(String.format("static/images/%s", srcImageName));
        InputStream inputStream = resource.getInputStream();
        boolean cutSuccess = false;
        Iterator<ImageReader> ite = ImageIO.getImageReadersByFormatName(formatName);
        if (ite.hasNext()) {
            ImageReader reader = ite.next();
            try(ImageInputStream iis = ImageIO.createImageInputStream(inputStream)) {
                reader.setInput(iis);
                ImageReadParam defaultReadParam = reader.getDefaultReadParam();

                // 制作图片锁
                BufferedImage lockBI = reader.read(0, defaultReadParam);
                int maxX = x + width;
                int maxY = y + height;
                int[] rgb = {16, 16, 16};
                for (int i = x; i < maxX; i++) {
                    for (int j = y ; j < maxY; j++) {
                        lockBI.setRGB(i, j, rgb[2]);
                    }
                }
                File lockFile = new File(String.format("%s/%s", imageDir, lock));
                lockFile.createNewFile();
                cutSuccess = ImageIO.write(lockBI, formatName, lockFile);

                // 制作图片钥匙
                Rectangle rec = new Rectangle(x, y, width, height);
                defaultReadParam.setSourceRegion(rec);
                BufferedImage keyBI = reader.read(0, defaultReadParam);
                File keyFile = new File(String.format("%s/%s", imageDir, key));
                keyFile.createNewFile();
                cutSuccess = ImageIO.write(keyBI, formatName, keyFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("不支持的格式[" + formatName + "]");
        }
        return cutSuccess;
    }


}
