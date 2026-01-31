package com.ndl.numbers_dont_lie.shoppinglist.dto;

import java.util.List;

public class DailyShoppingListResponse {
    private String date;
    private List<ShoppingListItemDto> items;

    public DailyShoppingListResponse() {}

    public DailyShoppingListResponse(String date, List<ShoppingListItemDto> items) {
        this.date = date;
        this.items = items;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<ShoppingListItemDto> getItems() {
        return items;
    }

    public void setItems(List<ShoppingListItemDto> items) {
        this.items = items;
    }
}
