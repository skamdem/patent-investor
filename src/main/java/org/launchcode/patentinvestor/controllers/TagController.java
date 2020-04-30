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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
public class TagController extends AbstractBaseController {

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
            List<String> reducedPagination = paginatedListingService.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
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
    public String processCreateTagForm(
            @Valid @ModelAttribute Tag tag,
            Errors errors,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute("title", "Create tag (investment field)");
            model.addAttribute(tag);
            model.addAttribute(MESSAGE_KEY, "danger|Failed to create a new investment field");
            return "tags/create";
        }
        tagRepository.save(tag);
        redirectAttributes.addFlashAttribute(MESSAGE_KEY, "success|New investment field " + tag.getDisplayName() + " created");
        return "redirect:/tags";
    }

    @GetMapping("delete")
    public String displayDeleteTagForm(Model model) {
        model.addAttribute("title", "Delete investment field");
        model.addAttribute("tags", tagRepository.findAll());
        return "tags/delete";
    }

    @PostMapping("delete")
    public String processDeleteTagForm(
            @RequestParam(required = false) int[] tagIds,
            RedirectAttributes redirectAttributes) {
        if (tagIds != null) {
            for (int id : tagIds) {
                //tagRepository
                tagRepository.deleteById(id);
            }
            redirectAttributes.addFlashAttribute(MESSAGE_KEY, "success|" + tagIds.length + " investment field(s) deleted");
            return "redirect:/tags";
        }
        redirectAttributes.addFlashAttribute(MESSAGE_KEY, "info|No investment field deleted");
        return "redirect:/tags";
    }

    @GetMapping("view/{tagId}")
    public String displayTag(
            Model model,
            @PathVariable int tagId) {
        Tag tag = tagRepository.findById(tagId).get();
        String tagName = tag.getDisplayName();
        model.addAttribute("title",
                "Investment field " + tagName);
        model.addAttribute("theTag", tag);
        if (tag.getStocks().size() > 0) {
            model.addAttribute(MESSAGE_KEY, "info|" +
                    tag.getDisplayName() +
                    " is currently set to " +
                    tag.getStocks().size() + " stock(s).");
        }
        return "tags/view";
    }

    @GetMapping("edit/{tagId}")
    public String displayEditForm(
            Model model,
            @PathVariable int tagId) {
        Tag tag = tagRepository.findById(tagId).get();
        String tagName = tag.getDisplayName();
        model.addAttribute("title",
                "Edit investment field " + tagName);
        model.addAttribute("theTag", tag);
        if (tag.getStocks().size() > 0) {
            model.addAttribute(MESSAGE_KEY, "warning|" +
                    tag.getDisplayName() +
                    " is currently set to " +
                    tag.getStocks().size() + " stock(s) that would be affected");
        }
        return "tags/edit";
    }

    @PostMapping("edit")
    public String processEditForm(
            int tagId,
            String name,
            String description,
            RedirectAttributes redirectAttributes) {
        Optional<Tag> result = tagRepository.findById(tagId);
        Tag tag = result.get();
        tag.setDescription(description);
        tag.setName(name);
        tagRepository.save(tag);
        redirectAttributes.addFlashAttribute(MESSAGE_KEY, "success|" + tag.getDisplayName() + " successfully edited");
        return "redirect:/tags";
    }
}
