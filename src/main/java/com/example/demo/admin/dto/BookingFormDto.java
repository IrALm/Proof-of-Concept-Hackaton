package com.example.demo.admin.dto;

/**
 * Payload du formulaire de création manuelle d'une réservation.
 * Utilisé par le template {@code admin/booking-new.html}.
 */
public class BookingFormDto {

    // ── Hôtel (choisi dans un dropdown) ───────────────────────────────
    private String hotel;
    private String country;
    private String marketSegment;

    // ── Statut ────────────────────────────────────────────────────────
    private Boolean isCanceled = false;
    private String reservationStatus = "Check-Out";
    private String reservationStatusDate;   // yyyy-MM-dd ; pré-rempli côté contrôleur

    // ── Arrivée ───────────────────────────────────────────────────────
    private Integer leadTime;
    private Integer arrivalDateYear;
    private String arrivalDateMonth;
    private Integer arrivalDateWeekNumber;
    private Integer arrivalDateDayOfMonth;

    // ── Durée du séjour ───────────────────────────────────────────────
    private Integer staysInWeekendNights = 0;
    private Integer staysInWeekNights = 1;

    // ── Voyageurs ─────────────────────────────────────────────────────
    private Integer adults = 2;
    private Integer children = 0;
    private Integer babies = 0;

    // ── Autres ────────────────────────────────────────────────────────
    private String meal = "BB";
    private String distributionChannel = "Direct";
    private Boolean isRepeatedGuest = false;
    private Integer previousCancellations = 0;
    private Integer previousBookingsNotCanceled = 0;
    private String reservedRoomType = "A";
    private String assignedRoomType = "A";
    private Integer bookingChanges = 0;
    private String depositType = "No Deposit";
    private String agent;
    private String company;
    private Integer daysInWaitingList = 0;
    private String customerType = "Transient";
    private Double adr;
    private Integer requiredCarParkingSpaces = 0;
    private Integer totalOfSpecialRequests = 0;

    // ── Getters / Setters ─────────────────────────────────────────────
    public String getHotel() { return hotel; }
    public void setHotel(String hotel) { this.hotel = hotel; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getMarketSegment() { return marketSegment; }
    public void setMarketSegment(String marketSegment) { this.marketSegment = marketSegment; }
    public Boolean getIsCanceled() { return isCanceled; }
    public void setIsCanceled(Boolean isCanceled) { this.isCanceled = isCanceled; }
    public String getReservationStatus() { return reservationStatus; }
    public void setReservationStatus(String reservationStatus) { this.reservationStatus = reservationStatus; }
    public String getReservationStatusDate() { return reservationStatusDate; }
    public void setReservationStatusDate(String reservationStatusDate) { this.reservationStatusDate = reservationStatusDate; }
    public Integer getLeadTime() { return leadTime; }
    public void setLeadTime(Integer leadTime) { this.leadTime = leadTime; }
    public Integer getArrivalDateYear() { return arrivalDateYear; }
    public void setArrivalDateYear(Integer arrivalDateYear) { this.arrivalDateYear = arrivalDateYear; }
    public String getArrivalDateMonth() { return arrivalDateMonth; }
    public void setArrivalDateMonth(String arrivalDateMonth) { this.arrivalDateMonth = arrivalDateMonth; }
    public Integer getArrivalDateWeekNumber() { return arrivalDateWeekNumber; }
    public void setArrivalDateWeekNumber(Integer arrivalDateWeekNumber) { this.arrivalDateWeekNumber = arrivalDateWeekNumber; }
    public Integer getArrivalDateDayOfMonth() { return arrivalDateDayOfMonth; }
    public void setArrivalDateDayOfMonth(Integer arrivalDateDayOfMonth) { this.arrivalDateDayOfMonth = arrivalDateDayOfMonth; }
    public Integer getStaysInWeekendNights() { return staysInWeekendNights; }
    public void setStaysInWeekendNights(Integer staysInWeekendNights) { this.staysInWeekendNights = staysInWeekendNights; }
    public Integer getStaysInWeekNights() { return staysInWeekNights; }
    public void setStaysInWeekNights(Integer staysInWeekNights) { this.staysInWeekNights = staysInWeekNights; }
    public Integer getAdults() { return adults; }
    public void setAdults(Integer adults) { this.adults = adults; }
    public Integer getChildren() { return children; }
    public void setChildren(Integer children) { this.children = children; }
    public Integer getBabies() { return babies; }
    public void setBabies(Integer babies) { this.babies = babies; }
    public String getMeal() { return meal; }
    public void setMeal(String meal) { this.meal = meal; }
    public String getDistributionChannel() { return distributionChannel; }
    public void setDistributionChannel(String distributionChannel) { this.distributionChannel = distributionChannel; }
    public Boolean getIsRepeatedGuest() { return isRepeatedGuest; }
    public void setIsRepeatedGuest(Boolean isRepeatedGuest) { this.isRepeatedGuest = isRepeatedGuest; }
    public Integer getPreviousCancellations() { return previousCancellations; }
    public void setPreviousCancellations(Integer previousCancellations) { this.previousCancellations = previousCancellations; }
    public Integer getPreviousBookingsNotCanceled() { return previousBookingsNotCanceled; }
    public void setPreviousBookingsNotCanceled(Integer previousBookingsNotCanceled) { this.previousBookingsNotCanceled = previousBookingsNotCanceled; }
    public String getReservedRoomType() { return reservedRoomType; }
    public void setReservedRoomType(String reservedRoomType) { this.reservedRoomType = reservedRoomType; }
    public String getAssignedRoomType() { return assignedRoomType; }
    public void setAssignedRoomType(String assignedRoomType) { this.assignedRoomType = assignedRoomType; }
    public Integer getBookingChanges() { return bookingChanges; }
    public void setBookingChanges(Integer bookingChanges) { this.bookingChanges = bookingChanges; }
    public String getDepositType() { return depositType; }
    public void setDepositType(String depositType) { this.depositType = depositType; }
    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public Integer getDaysInWaitingList() { return daysInWaitingList; }
    public void setDaysInWaitingList(Integer daysInWaitingList) { this.daysInWaitingList = daysInWaitingList; }
    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }
    public Double getAdr() { return adr; }
    public void setAdr(Double adr) { this.adr = adr; }
    public Integer getRequiredCarParkingSpaces() { return requiredCarParkingSpaces; }
    public void setRequiredCarParkingSpaces(Integer requiredCarParkingSpaces) { this.requiredCarParkingSpaces = requiredCarParkingSpaces; }
    public Integer getTotalOfSpecialRequests() { return totalOfSpecialRequests; }
    public void setTotalOfSpecialRequests(Integer totalOfSpecialRequests) { this.totalOfSpecialRequests = totalOfSpecialRequests; }
}
