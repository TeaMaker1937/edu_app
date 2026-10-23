package edu.platform.catalog;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("courses")
public class Course {

    @Id
    private Long id;

    private String title;

    private String summary;

    private BigDecimal price;

    @Column("teacher_id")
    private Long teacherId;

    @Column("reserved_seats")
    private Integer reservedSeats;

    // конструкторы, геттеры, сеттеры
    public Course() {}

    public Course(Long id, String title, String summary, BigDecimal price, Long teacherId, Integer reservedSeats) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.price = price;
        this.teacherId = teacherId;
        this.reservedSeats = reservedSeats;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public Integer getReservedSeats() { return reservedSeats; }
    public void setReservedSeats(Integer reservedSeats) { this.reservedSeats = reservedSeats; }
}