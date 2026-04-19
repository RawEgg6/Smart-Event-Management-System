package com.smartevent.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import java.nio.file.*;

@Service
public class QRService {
    public String generateQR(String text, Long ticketId) throws Exception {
        String folder = "src/main/resources/static/qr/";
        Files.createDirectories(Paths.get(folder));
        String path = folder + "ticket-" + ticketId + ".png";
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 250, 250);
        MatrixToImageWriter.writeToPath(matrix, "PNG", Paths.get(path));
        return "/qr/ticket-" + ticketId + ".png";
    }
}