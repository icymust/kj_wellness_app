package com.ndl.numbers_dont_lie.shoppinglist.dto;

import java.util.List;

public class WeeklyShoppingListResponse {
    private String startDate;
    private String endDate;
    private List<ShoppingListItemDto> items;

    public WeeklyShoppingListResponse() {}

    public WeeklyShoppingListResponse(String startDate, String endDate, List<ShoppingListItemDto> items) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.items = items;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<ShoppingListItemDto> getItems() {
        return items;
    }

    public void setItems(List<ShoppingListItemDto> items) {
        this.items = items;
    }
}
