package com.xb.rag.document.parser;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * OCR 服务 —— 基于 Tess4J 的简单封装
 *
 * @author ibqy
 */
public class OcrService {

    private final ITesseract tesseract;

    public OcrService() {
        this.tesseract = new Tesseract();
        // 默认不设 dataPath，由系统环境 TESSDATA_PREFIX 指定
    }

    public OcrService(String tessDataPath) {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(tessDataPath);
    }

    /**
     * 对图片输入流进行 OCR 识别
     * @param image  图片输入流
     * @param lang   语言代码，如 "chi_sim+eng"
     * @return 识别文本
     */
    public String doOcr(InputStream image, String lang) {
        try {
            BufferedImage buf = ImageIO.read(image);
            if (buf == null) {
                return "";
            }
            tesseract.setLanguage(lang != null ? lang : "eng");
            return tesseract.doOCR(buf);
        } catch (IOException | TesseractException e) {
            throw new RuntimeException("OCR 识别失败: " + e.getMessage(), e);
        }
    }
}