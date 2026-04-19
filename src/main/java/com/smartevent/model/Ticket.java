package com.smartevent.model;

import jakarta.persistence.*;

@Entity
public class Ticket {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String studentName;
    private String qrPath;
    @ManyToOne private Event event;
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getStudentName(){return studentName;} public void setStudentName(String s){this.studentName=s;}
    public String getQrPath(){return qrPath;} public void setQrPath(String q){this.qrPath=q;}
    public Event getEvent(){return event;} public void setEvent(Event e){this.event=e;}
}