package org.launchcode.patentinvestor.controllers;

import org.launchcode.patentinvestor.data.TagRepository;
import org.launchcode.patentinvestor.models.PaginatedListingService;
import org.launchcode.patentinvestor.models.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by kamdem
 */
@Controller
@RequestMapping("tags")
public class TagController {

    static final int numberOfItemsPerPage = 10;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PaginatedListingService<Tag> paginatedListingService;

    @RequestMapping(method = RequestMethod.GET)
    public String displayAllTags(
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);
        model.addAttribute("title", "All investment fields");

        paginatedListingService.setListOfItems((List<Tag>) tagRepository.findAll());
        Page<Tag> tagPage = paginatedListingService.findPaginated(PageRequest.of(currentPage - 1, pageSize));

        model.addAttribute("tagPage", tagPage);

        int totalPages = tagPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        //model.addAttribute("tags", tagRepository.findAll());
        return "tags/index";
    }

    @GetMapping("create")
    public String renderCreateTagForm(Model model) {
        model.addAttribute("title", "Create Tag (Investment field)");
        model.addAttribute(new Tag());
        return "tags/create";
    }

    @PostMapping("create")
    public String processCreateTagForm(@Valid @ModelAttribute Tag tag,
                                       Errors errors, Model model) {

        if (errors.hasErrors()) {
            model.addAttribute("title", "Create Tag (Investment field)");
            model.addAttribute(tag);
            return "tags/create";
        }

        tagRepository.save(tag);
        return "redirect:";
    }

    @GetMapping("delete")
    public String displayDeleteTagForm(Model model) {
        model.addAttribute("title", "Delete investment field");
        model.addAttribute("tags", tagRepository.findAll());
        return "tags/delete";
    }

    @PostMapping("delete")
    public String processDeleteTagForm(@RequestParam(required = false) int[] tagIds) {
        if (tagIds != null) {
            for (int id : tagIds) {
                //tagRepository
                tagRepository.deleteById(id);
            }
        }
        return "redirect:";
    }

    @GetMapping("edit/{tagId}")
    public String displayEditForm(Model model, @PathVariable int tagId) {
        String tagName = tagRepository.findById(tagId).get().getDisplayName();
        model.addAttribute("title",
                "Edit investment field " + tagName);
        model.addAttribute("theTag", tagRepository.findById(tagId).get());
        return "tags/edit";
    }

    @PostMapping("edit")
    public String processEditForm(int tagId, String name, String description) {
        Optional<Tag> result = tagRepository.findById(tagId);
        Tag tag = result.get();
        tag.setDescription(description);
        tag.setName(name);
        tagRepository.save(tag);
        return "redirect:";
    }
}
