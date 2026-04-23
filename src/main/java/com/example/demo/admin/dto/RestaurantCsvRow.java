package com.example.demo.admin.dto;

import com.opencsv.bean.CsvBindByName;

/**
 * Mapping d'une ligne du CSV restaurants.
 * Les colonnes attendues correspondent au CSV Michelin original :
 * Name, Address, Location, Price, Cuisine, Longitude, Latitude,
 * PhoneNumber, Url, WebsiteUrl, Award, GreenStar, FacilitiesAndServices, Description
 */
public class RestaurantCsvRow {

    @CsvBindByName(column = "Name")
    private String name;

    @CsvBindByName(column = "Address")
    private String address;

    @CsvBindByName(column = "Location")
    private String location;

    @CsvBindByName(column = "Price")
    private String price;

    @CsvBindByName(column = "Cuisine")
    private String cuisine;

    @CsvBindByName(column = "Longitude")
    private String longitude;

    @CsvBindByName(column = "Latitude")
    private String latitude;

    @CsvBindByName(column = "PhoneNumber")
    private String phoneNumber;

    @CsvBindByName(column = "Url")
    private String url;

    @CsvBindByName(column = "WebsiteUrl")
    private String websiteUrl;

    @CsvBindByName(column = "Award")
    private String award;

    @CsvBindByName(column = "GreenStar")
    private String greenStar;

    @CsvBindByName(column = "FacilitiesAndServices")
    private String facilitiesAndServices;

    @CsvBindByName(column = "Description")
    private String description;

    // ── Getters / Setters ─────────────────────────────────────────────
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getAward() { return award; }
    public void setAward(String award) { this.award = award; }

    public String getGreenStar() { return greenStar; }
    public void setGreenStar(String greenStar) { this.greenStar = greenStar; }

    public String getFacilitiesAndServices() { return facilitiesAndServices; }
    public void setFacilitiesAndServices(String facilitiesAndServices) { this.facilitiesAndServices = facilitiesAndServices; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
