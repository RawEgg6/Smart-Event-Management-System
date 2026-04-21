package com.smart.event.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import com.smart.event.entity.AttendanceRecord;
import com.smart.event.entity.Event;
import com.smart.event.repository.AttendanceRecordRepository;
import com.smart.event.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    public Map<String, Long> countEventsByStatus() {
        return eventRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        e -> e.getStatus() != null ? e.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));
    }

    public Map<String, Long> checkinCountPerEvent() {
        return attendanceRecordRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        r -> r.getEvent().getTitle(),
                        Collectors.counting()
                ));
    }

    public List<String> getAllOrganizerNames() {
        return eventRepository.findAll().stream()
                .map(e -> e.getOrganizer() != null ? e.getOrganizer().getUsername() : "")
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<Event> filterEvents(LocalDate from, LocalDate to, String organizer) {
        return eventRepository.findAll().stream()
                .filter(e -> from == null || (e.getStartTime() != null &&
                        !e.getStartTime().toLocalDate().isBefore(from)))
                .filter(e -> to == null || (e.getStartTime() != null &&
                        !e.getStartTime().toLocalDate().isAfter(to)))
                .filter(e -> organizer == null || organizer.isBlank() ||
                        (e.getOrganizer() != null && organizer.equals(e.getOrganizer().getUsername())))
                .collect(Collectors.toList());
    }

    public void exportCsv(LocalDate from, LocalDate to, String organizer, Writer writer) throws IOException {
        List<Event> events = filterEvents(from, to, organizer);
        try (CSVWriter csv = new CSVWriter(writer)) {
            csv.writeNext(new String[]{"Event ID", "Event Title", "Start Time", "Status",
                    "Capacity", "Check-ins", "Organizer"});
            for (Event e : events) {
                long checkins = attendanceRecordRepository.countByEventId(e.getId());
                csv.writeNext(new String[]{
                        String.valueOf(e.getId()),
                        e.getTitle(),
                        e.getStartTime() != null ? e.getStartTime().toString() : "",
                        e.getStatus() != null ? e.getStatus() : "",
                        String.valueOf(e.getCapacity()),
                        String.valueOf(checkins),
                        e.getOrganizer() != null ? e.getOrganizer().getUsername() : ""
                });
            }
        }
    }

    public void exportPdf(LocalDate from, LocalDate to, String organizer, OutputStream out) throws IOException {
        List<Event> events = filterEvents(from, to, organizer);
        try {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titleFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font bodyFont   = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph("Smart Event Management — Report", titleFont));
            doc.add(new Paragraph("Generated: " + LocalDate.now(), bodyFont));
            doc.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4, 14, 10, 8, 6, 6, 10});

            for (String h : new String[]{"ID", "Event Title", "Start Time", "Status", "Capacity", "Check-ins", "Organizer"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            for (Event e : events) {
                long checkins = attendanceRecordRepository.countByEventId(e.getId());
                table.addCell(new Phrase(String.valueOf(e.getId()), bodyFont));
                table.addCell(new Phrase(e.getTitle(), bodyFont));
                table.addCell(new Phrase(e.getStartTime() != null ? e.getStartTime().toString() : "", bodyFont));
                table.addCell(new Phrase(e.getStatus() != null ? e.getStatus() : "", bodyFont));
                table.addCell(new Phrase(String.valueOf(e.getCapacity()), bodyFont));
                table.addCell(new Phrase(String.valueOf(checkins), bodyFont));
                table.addCell(new Phrase(e.getOrganizer() != null ? e.getOrganizer().getUsername() : "", bodyFont));
            }

            doc.add(table);
            doc.close();
        } catch (DocumentException ex) {
            throw new IOException("PDF generation failed", ex);
        }
    }
}