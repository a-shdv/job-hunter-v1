package com.company.aggregator.dto;

import com.company.aggregator.model.Favourite;
import lombok.Builder;
import lombok.Getter;

@Builder
public record FavouriteDto(@Getter String title, @Getter String date, @Getter String company, @Getter String schedule,
                           @Getter String source) {
    public FavouriteDto(String title, String date, String company, String schedule, String source) {
        this.title = title;
        this.date = date;
        this.company = company;
        this.schedule = schedule;
        this.source = source;
    }

    public static Favourite toFavourite(FavouriteDto favouriteDto) {
        return Favourite.builder()
                .title(favouriteDto.getTitle())
                .date(favouriteDto.getDate())
                .company(favouriteDto.getCompany())
                .schedule(favouriteDto.getSchedule())
                .source(favouriteDto.getSource())
                .build();
    }
}
