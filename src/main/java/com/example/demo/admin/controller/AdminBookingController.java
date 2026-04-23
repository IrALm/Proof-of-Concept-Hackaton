package com.example.demo.admin.controller;

import com.example.demo.admin.dto.BookingFormDto;
import com.example.demo.service.SupabaseDbService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Création manuelle d'une réservation dans {@code hotel_bookings}.
 * L'hôtel est choisi dans un dropdown alimenté par la table {@code hotels}.
 */
@Controller
@RequestMapping("${app.admin.base-path}/bookings")
public class AdminBookingController {

    private final SupabaseDbService dbService;

    public AdminBookingController(SupabaseDbService dbService) {
        this.dbService = dbService;
    }

    // ─── GET : formulaire pré-rempli ──────────────────────────────────
    @GetMapping("/new")
    public String showForm(Model model) {
        model.addAttribute("hotels", dbService.findAllHotels());
        model.addAttribute("form", defaultForm());
        return "admin/booking-new";
    }

    // ─── POST : création ──────────────────────────────────────────────
    @PostMapping
    public String createBooking(@ModelAttribute("form") BookingFormDto form, Model model) {

        try {
            Map<String, Object> payload = toPayload(form);
            dbService.insertHotelBooking(payload);
            model.addAttribute("success", "Réservation créée avec succès.");
            model.addAttribute("form", defaultForm());   // reset
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de la création : " + e.getMessage());
            model.addAttribute("form", form);            // garde les valeurs saisies
        }

        model.addAttribute("hotels", dbService.findAllHotels());
        return "admin/booking-new";
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private BookingFormDto defaultForm() {
        BookingFormDto f = new BookingFormDto();
        LocalDate today = LocalDate.now();
        f.setReservationStatusDate(today.toString());
        f.setArrivalDateYear(today.getYear());
        f.setArrivalDateMonth(today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        f.setArrivalDateDayOfMonth(today.getDayOfMonth());
        f.setArrivalDateWeekNumber(today.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()));
        f.setLeadTime(0);
        f.setAdr(100.0);
        return f;
    }

    private Map<String, Object> toPayload(BookingFormDto f) {
        Map<String, Object> p = new HashMap<>();
        p.put("hotel", f.getHotel());
        p.put("is_canceled", Boolean.TRUE.equals(f.getIsCanceled()));
        p.put("lead_time", f.getLeadTime());
        p.put("arrival_date_year", f.getArrivalDateYear());
        p.put("arrival_date_month", f.getArrivalDateMonth());
        p.put("arrival_date_week_number", f.getArrivalDateWeekNumber());
        p.put("arrival_date_day_of_month", f.getArrivalDateDayOfMonth());
        p.put("stays_in_weekend_nights", f.getStaysInWeekendNights());
        p.put("stays_in_week_nights", f.getStaysInWeekNights());
        p.put("adults", f.getAdults());
        p.put("children", f.getChildren());
        p.put("babies", f.getBabies());
        p.put("meal", f.getMeal());
        p.put("country", f.getCountry());
        p.put("market_segment", f.getMarketSegment());
        p.put("distribution_channel", f.getDistributionChannel());
        p.put("is_repeated_guest", Boolean.TRUE.equals(f.getIsRepeatedGuest()));
        p.put("previous_cancellations", f.getPreviousCancellations());
        p.put("previous_bookings_not_canceled", f.getPreviousBookingsNotCanceled());
        p.put("reserved_room_type", f.getReservedRoomType());
        p.put("assigned_room_type", f.getAssignedRoomType());
        p.put("booking_changes", f.getBookingChanges());
        p.put("deposit_type", f.getDepositType());
        p.put("agent", f.getAgent());
        p.put("company", f.getCompany());
        p.put("days_in_waiting_list", f.getDaysInWaitingList());
        p.put("customer_type", f.getCustomerType());
        p.put("adr", f.getAdr());
        p.put("required_car_parking_spaces", f.getRequiredCarParkingSpaces());
        p.put("total_of_special_requests", f.getTotalOfSpecialRequests());
        p.put("reservation_status", f.getReservationStatus());
        p.put("reservation_status_date", f.getReservationStatusDate());
        return p;
    }
}
