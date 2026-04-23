package com.example.demo.admin.dto;

import com.opencsv.bean.CsvBindByName;

/**
 * Mapping d'une ligne du CSV hotel_bookings (dataset Kaggle "hotel-booking-demand").
 * Chaque ligne représente une réservation ; les hôtels uniques sont ensuite extraits
 * par agrégation côté {@code HotelImportService}.
 */
public class HotelBookingCsvRow {

    @CsvBindByName(column = "hotel")
    private String hotel;

    @CsvBindByName(column = "is_canceled")
    private String isCanceled;

    @CsvBindByName(column = "lead_time")
    private String leadTime;

    @CsvBindByName(column = "arrival_date_year")
    private String arrivalDateYear;

    @CsvBindByName(column = "arrival_date_month")
    private String arrivalDateMonth;

    @CsvBindByName(column = "arrival_date_week_number")
    private String arrivalDateWeekNumber;

    @CsvBindByName(column = "arrival_date_day_of_month")
    private String arrivalDateDayOfMonth;

    @CsvBindByName(column = "stays_in_weekend_nights")
    private String staysInWeekendNights;

    @CsvBindByName(column = "stays_in_week_nights")
    private String staysInWeekNights;

    @CsvBindByName(column = "adults")
    private String adults;

    @CsvBindByName(column = "children")
    private String children;

    @CsvBindByName(column = "babies")
    private String babies;

    @CsvBindByName(column = "meal")
    private String meal;

    @CsvBindByName(column = "country")
    private String country;

    @CsvBindByName(column = "market_segment")
    private String marketSegment;

    @CsvBindByName(column = "distribution_channel")
    private String distributionChannel;

    @CsvBindByName(column = "is_repeated_guest")
    private String isRepeatedGuest;

    @CsvBindByName(column = "previous_cancellations")
    private String previousCancellations;

    @CsvBindByName(column = "previous_bookings_not_canceled")
    private String previousBookingsNotCanceled;

    @CsvBindByName(column = "reserved_room_type")
    private String reservedRoomType;

    @CsvBindByName(column = "assigned_room_type")
    private String assignedRoomType;

    @CsvBindByName(column = "booking_changes")
    private String bookingChanges;

    @CsvBindByName(column = "deposit_type")
    private String depositType;

    @CsvBindByName(column = "agent")
    private String agent;

    @CsvBindByName(column = "company")
    private String company;

    @CsvBindByName(column = "days_in_waiting_list")
    private String daysInWaitingList;

    @CsvBindByName(column = "customer_type")
    private String customerType;

    @CsvBindByName(column = "adr")
    private String adr;

    @CsvBindByName(column = "required_car_parking_spaces")
    private String requiredCarParkingSpaces;

    @CsvBindByName(column = "total_of_special_requests")
    private String totalOfSpecialRequests;

    @CsvBindByName(column = "reservation_status")
    private String reservationStatus;

    @CsvBindByName(column = "reservation_status_date")
    private String reservationStatusDate;

    // ── Getters / Setters ─────────────────────────────────────────────
    public String getHotel() { return hotel; }
    public void setHotel(String hotel) { this.hotel = hotel; }
    public String getIsCanceled() { return isCanceled; }
    public void setIsCanceled(String isCanceled) { this.isCanceled = isCanceled; }
    public String getLeadTime() { return leadTime; }
    public void setLeadTime(String leadTime) { this.leadTime = leadTime; }
    public String getArrivalDateYear() { return arrivalDateYear; }
    public void setArrivalDateYear(String arrivalDateYear) { this.arrivalDateYear = arrivalDateYear; }
    public String getArrivalDateMonth() { return arrivalDateMonth; }
    public void setArrivalDateMonth(String arrivalDateMonth) { this.arrivalDateMonth = arrivalDateMonth; }
    public String getArrivalDateWeekNumber() { return arrivalDateWeekNumber; }
    public void setArrivalDateWeekNumber(String arrivalDateWeekNumber) { this.arrivalDateWeekNumber = arrivalDateWeekNumber; }
    public String getArrivalDateDayOfMonth() { return arrivalDateDayOfMonth; }
    public void setArrivalDateDayOfMonth(String arrivalDateDayOfMonth) { this.arrivalDateDayOfMonth = arrivalDateDayOfMonth; }
    public String getStaysInWeekendNights() { return staysInWeekendNights; }
    public void setStaysInWeekendNights(String staysInWeekendNights) { this.staysInWeekendNights = staysInWeekendNights; }
    public String getStaysInWeekNights() { return staysInWeekNights; }
    public void setStaysInWeekNights(String staysInWeekNights) { this.staysInWeekNights = staysInWeekNights; }
    public String getAdults() { return adults; }
    public void setAdults(String adults) { this.adults = adults; }
    public String getChildren() { return children; }
    public void setChildren(String children) { this.children = children; }
    public String getBabies() { return babies; }
    public void setBabies(String babies) { this.babies = babies; }
    public String getMeal() { return meal; }
    public void setMeal(String meal) { this.meal = meal; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getMarketSegment() { return marketSegment; }
    public void setMarketSegment(String marketSegment) { this.marketSegment = marketSegment; }
    public String getDistributionChannel() { return distributionChannel; }
    public void setDistributionChannel(String distributionChannel) { this.distributionChannel = distributionChannel; }
    public String getIsRepeatedGuest() { return isRepeatedGuest; }
    public void setIsRepeatedGuest(String isRepeatedGuest) { this.isRepeatedGuest = isRepeatedGuest; }
    public String getPreviousCancellations() { return previousCancellations; }
    public void setPreviousCancellations(String previousCancellations) { this.previousCancellations = previousCancellations; }
    public String getPreviousBookingsNotCanceled() { return previousBookingsNotCanceled; }
    public void setPreviousBookingsNotCanceled(String previousBookingsNotCanceled) { this.previousBookingsNotCanceled = previousBookingsNotCanceled; }
    public String getReservedRoomType() { return reservedRoomType; }
    public void setReservedRoomType(String reservedRoomType) { this.reservedRoomType = reservedRoomType; }
    public String getAssignedRoomType() { return assignedRoomType; }
    public void setAssignedRoomType(String assignedRoomType) { this.assignedRoomType = assignedRoomType; }
    public String getBookingChanges() { return bookingChanges; }
    public void setBookingChanges(String bookingChanges) { this.bookingChanges = bookingChanges; }
    public String getDepositType() { return depositType; }
    public void setDepositType(String depositType) { this.depositType = depositType; }
    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getDaysInWaitingList() { return daysInWaitingList; }
    public void setDaysInWaitingList(String daysInWaitingList) { this.daysInWaitingList = daysInWaitingList; }
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }
    public String getAdr() { return adr; }
    public void setAdr(String adr) { this.adr = adr; }
    public String getRequiredCarParkingSpaces() { return requiredCarParkingSpaces; }
    public void setRequiredCarParkingSpaces(String requiredCarParkingSpaces) { this.requiredCarParkingSpaces = requiredCarParkingSpaces; }
    public String getTotalOfSpecialRequests() { return totalOfSpecialRequests; }
    public void setTotalOfSpecialRequests(String totalOfSpecialRequests) { this.totalOfSpecialRequests = totalOfSpecialRequests; }
    public String getReservationStatus() { return reservationStatus; }
    public void setReservationStatus(String reservationStatus) { this.reservationStatus = reservationStatus; }
    public String getReservationStatusDate() { return reservationStatusDate; }
    public void setReservationStatusDate(String reservationStatusDate) { this.reservationStatusDate = reservationStatusDate; }
}
