package com.company.parser.service;

import com.company.parser.rabbitmq.dto.SendMessageDto;
import com.company.parser.rabbitmq.service.RabbitMqSenderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class HabrParserService {
    private final RabbitMqSenderService rabbitMqSenderService;
    private static final int vacanciesPerPage = 25;

    public CompletableFuture<Void> findAllVacancies(String query, int amount, BigDecimal salary, boolean onlyWithSalary,
                                                    int experience,int cityId, boolean isRemoteAvailable) {
        return CompletableFuture.runAsync(() -> {
            int previousPage;
            int currentPage = 1;
            StringBuilder url = new StringBuilder("https://career.habr.com/vacancies" +
                    "?page=" + currentPage + "&q=" + query + "&salary=" + salary + "&with_salary=" + onlyWithSalary +
                    "&city_id=" + parseCityId(cityId) + "&remote=" + isRemoteAvailable + "&type=all");

            int parsedExperience = parseExperience(experience);
            if (parsedExperience != -1)  {
                url.append("&qid=").append(parsedExperience);
            }

            Document doc = null;
            while (currentPage <= amount / vacanciesPerPage) {
                try {
                    doc = Jsoup.connect(url.toString()).get();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }

                if (doc != null) {
                    Elements sections = doc.getElementsByClass("section-group section-group--gap-medium").last().getElementsByClass("section-box");
                    for (Element section : sections) {
                        String vacancyUrl = section.getElementsByClass("vacancy-card__title-link").first().absUrl("href");

                        SendMessageDto sendMessageDto = SendMessageDto.builder()
                                .title(section.getElementsByClass("vacancy-card__title").text())
                                .date(section.getElementsByClass("vacancy-card__date").text())
                                .salary(section.getElementsByClass("vacancy-card__salary").text())
                                .company(section.getElementsByClass("vacancy-card__company-title").text())
                                .requirements(section.getElementsByClass("vacancy-card__skills").first().text())
                                .schedule(section.getElementsByClass("vacancy-card__meta").text())
                                .description(parseWebPageDescription(vacancyUrl))
                                .source(vacancyUrl)
                                .build();

                        rabbitMqSenderService.send(sendMessageDto);
                    }
                    previousPage = currentPage;
                    currentPage++;

                    url.replace(
                            url.indexOf("?page=" + previousPage),
                            url.lastIndexOf("?page=" + previousPage),
                            "?page=" + currentPage
                    );

//                    url = "https://career.habr.com/vacancies?page=" + currentPage + "&q=" + query + "&type=all" + "&salary=" + salary;
                }
            }
        });
    }

    private int parseCityId(int cityId) {
        int parsedCityId = 0;
        switch (cityId) {
            case 0 -> parsedCityId = 678; // Москва
            case 1 -> parsedCityId = 679; // СПБ
            case 2 -> parsedCityId = 693; // ЕКБ
            case 3 -> parsedCityId = 717; // Новосибирск
            case 4 -> parsedCityId = 698; // Казань
            case 5 -> parsedCityId = 715; // Нижний Новгород
            case 6 -> parsedCityId = 739; // Ульяновск
            case 7 -> parsedCityId = 735; // Тольятти
            case 8 -> parsedCityId = 683; // Астрахань
            case 9 -> parsedCityId = 740; // Уфа
        }
        return parsedCityId;
    }

    private int parseExperience(int experience) {
        int parsedExperience = 0;
        switch (experience) {
            case 0 -> parsedExperience = -1; // 0 - не имеет значения
            case 1 -> parsedExperience = 1; // 1 - нет опыта
            case 2 -> parsedExperience = 3; // 2 - от 1 года до 3 лет
            case 3 -> parsedExperience = 4; // 3 - от 3 до 6 лет
            case 4 -> parsedExperience = 5;// 4 - более 6 лет
        }
        return parsedExperience;
    }

    private String parseWebPageDescription(String url) {
        Document doc = null;
        try {
            doc = Jsoup.connect(url).get();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        if (doc != null) {
            return doc.select("html body.vacancies_show_page div.page-container div.page-container__main div.page-width.page-width--responsive div.content-wrapper div.content-wrapper__main.content-wrapper__main--left section").get(1).text();
        }
        return null;
    }
}
