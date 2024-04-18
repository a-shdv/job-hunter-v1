package com.company.aggregator.controller;

import com.company.aggregator.entity.Request;
import com.company.aggregator.entity.User;
import com.company.aggregator.entity.Vacancy;
import com.company.aggregator.exception.VacancyNotFoundException;
import com.company.aggregator.rabbitmq.dto.CancelParsingDto;
import com.company.aggregator.rabbitmq.dto.vacancies.SendMessageDto;
import com.company.aggregator.rabbitmq.service.RabbitMqService;
import com.company.aggregator.service.RequestService;
import com.company.aggregator.service.UserService;
import com.company.aggregator.service.VacancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vacancies")
@RequiredArgsConstructor
@Slf4j
public class VacancyController {
    private final VacancyService vacancyService;
    private final RabbitMqService rabbitMqService;
    private final RestTemplate restTemplate;
    private final UserService userService;
    private final RequestService requestService;
    @Value("${constants.vacancies-heartbeat-url}")
    private String vacanciesHeartbeatUrl;
    private boolean isVacanciesParserAvailable;

    @GetMapping("/{id}")
    public String findVacancy(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Vacancy vacancy = null;
        try {
            vacancy = vacancyService.findById(id);
        } catch (VacancyNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
        model.addAttribute("vacancy", vacancy);
        return "vacancies/vacancy";
    }

    @GetMapping
    public String findVacancies(@AuthenticationPrincipal User user,
                                @RequestParam(required = false, defaultValue = "0") int page,
                                @RequestParam(required = false, defaultValue = "12") int size,
                                Model model) {
        String success = (String) model.getAttribute("success");
        String error = (String) model.getAttribute("error");
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (success != null) {
            model.addAttribute("success", success);
        }

        Page<Vacancy> vacancies = vacancyService.findVacancies(user, PageRequest.of(page, size));
        model.addAttribute("vacancies", vacancies);
        model.addAttribute("isParserAvailable", isVacanciesParserAvailable);
        return "vacancies/vacancies";
    }

    @PostMapping("/clear")
    public ResponseEntity<String> deleteVacancies(@AuthenticationPrincipal User user) {
        vacancyService.deleteVacanciesByUser(user);
        user.setNumOfRequests(0);
        userService.saveUser(user);
        return ResponseEntity.ok().body("Vacancies cleared successfully");
    }

    @PostMapping("/cancel-parsing")
    public String cancelParsing() {
        CancelParsingDto cancelParsingDto = CancelParsingDto.builder().isParsingCancelled(true).build();
        rabbitMqService.sendToVacanciesParserCancel(cancelParsingDto);
        return "/home";
    }

    @PostMapping("/search-next")
    public ResponseEntity<?> searchNext(@AuthenticationPrincipal User user) {
        Request request = requestService.findByUser(user);
        Integer numOfRequests = request.getNumOfRequests();
        numOfRequests = numOfRequests + 1;
        request.setNumOfRequests(numOfRequests);
        requestService.save(request);

        rabbitMqService.sendToVacanciesParser(SendMessageDto.builder()
                .username(request.getUser().getUsername())
                .title(request.getTitle())
                .salary(request.getSalary())
                .onlyWithSalary(request.getOnlyWithSalary())
                .experience(request.getExperience())
                .cityId(request.getCityId())
                .isRemoteAvailable(request.getIsRemoteAvailable())
                .numOfRequests(numOfRequests)
                .build());

        return ResponseEntity.ok().body("ok");
    }

    @Scheduled(initialDelay = 2_000, fixedDelay = 10_000)
    public void sendHeartBeat() {
        try {
            restTemplate.getForEntity(vacanciesHeartbeatUrl, String.class).getStatusCode().is2xxSuccessful();
            isVacanciesParserAvailable = true;
        } catch (ResourceAccessException ex) {
            isVacanciesParserAvailable = false;
        }
    }
}
