package com.soycasino.json;

public class CreateCustomer {
    public String first_name;
    public String last_name;
    public Address address;

    public CreateCustomer(String first_name, String last_name, Address address) {
        this.first_name = first_name;
        this.last_name = last_name;
        this.address = address;
    }
}
