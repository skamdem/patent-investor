package org.launchcode.patentinvestor.controllers;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.launchcode.patentinvestor.data.StockRepository;
import org.launchcode.patentinvestor.data.TagRepository;
import org.launchcode.patentinvestor.models.*;
import org.launchcode.patentinvestor.models.dto.StockTagDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.launchcode.patentinvestor.models.ApiData.*;

/**
 * Created by kamdem
 */
@Controller
@RequestMapping("stocks")
public class StockController extends AbstractBaseController {

    List<Stock> listOfStocksFound;
    static final int numberOfItemsPerPage = 10;
    static final String baseColor = "white";
    static final String selectedColor = "green";
    static String ordinarySortCriteria = "";
    static String inPortfolioSortCriteria = "";
    static String searchDestinationUrl = "/stocks/searchResults";

    static HashMap<String, String> columnChoices = new HashMap<>();
    static HashMap<String, String> stockExchanges = new HashMap<>();

    public StockController() {
        columnChoices.put("all", "All");
        columnChoices.put("ticker", "Ticker");
        columnChoices.put("companyName", "Company Name");

        stockExchanges.put("NAS", "Nasdaq Stock Market");
        stockExchanges.put("NYS", "New York Stock Exchange");
        stockExchanges.put("PSE", "Pacific Stock Exchange");
        stockExchanges.put("ASE", "American Stock Exchange");
        stockExchanges.put("BATS", "Bats Global Markets");
        stockExchanges.put("OTC", "Over-the-counter");
    }

    @ModelAttribute(name = "columns")
    public static HashMap<String, String> getColumnChoices() {
        return columnChoices;
    }

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private PaginatedListingService<Stock> paginatedListingService;

    /**
     * Display initial empty "Stock search form"
     * <p>
     * This method returns at URL /stocks/search
     */
    @RequestMapping(value = "search", method = RequestMethod.GET)
    public String displaySearchForm(Model model) {
        model.addAttribute("searchDestinationUrl", searchDestinationUrl);
        model.addAttribute("columns", columnChoices);
        model.addAttribute("title", "Search stock");
        return "stocks/search";
    }

    /**
     * Display ALL stocks SEARCH results (with pagination)
     * <p>
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     * <p>
     * "listingResults" works with: stockPage
     * <p>
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     * <p>
     * This method returns at URL /stocks/search
     */
    //@PostMapping(value = "searchResults")
    @RequestMapping(value = "searchResults", method = {RequestMethod.GET, RequestMethod.POST})
    public String displaySearchResults(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String searchType,
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);
        String iconsDestinationUrl = "/stocks/reorderedResults/?sortIcon=";
        String paginationDestinationUrl = "/stocks/searchResults/?size=";

        model.addAttribute("baseColor", baseColor);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("searchDestinationUrl", searchDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);
        //model.addAttribute("currentPage", currentPage);

        if (searchTerm != null) {
            if (searchTerm.toLowerCase().equals("all") || searchTerm == "") {
                //Release ALL stocks
                model.addAttribute("title", "All Stocks");
                listOfStocksFound = new ArrayList<Stock>((List<Stock>) stockRepository.findAll());
            } else {
                listOfStocksFound = StockData.findByColumnAndValue(searchType, searchTerm, (List<Stock>) stockRepository.findAll());
                //used to display the form with previous radio button in search.html
                model.addAttribute("searchType", searchType);
            }
        }

        model.addAttribute("listOfStocksFound", listOfStocksFound);
        paginatedListingService.setListOfItems(listOfStocksFound);

        //used to display the form in search.html
        model.addAttribute("columns", columnChoices);

        model.addAttribute("title", "Found stock(s)");

        Page<Stock> stockPage = paginatedListingService.findPaginated(PageRequest.of(currentPage - 1, pageSize));
        model.addAttribute("stockPage", stockPage);
        int totalPages = stockPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            //model.addAttribute("pageNumbers", pageNumbers);
            List<String> reducedPagination = paginatedListingService.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }
        return "stocks/search";
        //return "redirect:/search/results";
    }

    /**
     * Display ALL stocks search results (with pagination)
     * /stocks/results
     * Precondition : listOfStocksFound contains values
     * <p>
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     * <p>
     * "listingResults" works with: stockPage
     * <p>
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     * <p>
     * This method returns at URL /stocks/search
     */
    @GetMapping(value = "reorderedResults")
    public String displayReorderedSearchResults(
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size,
            @RequestParam("sortIcon") Optional<String> sortIcon) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);
        ordinarySortCriteria = sortIcon.orElse(ordinarySortCriteria);
        String iconsDestinationUrl = "/stocks/reorderedResults/?sortIcon=";
        String paginationDestinationUrl = "/stocks/reorderedResults/?size=";

        model.addAttribute("baseColor", baseColor);
        model.addAttribute("sortIcon", ordinarySortCriteria);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("searchDestinationUrl", searchDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);
        //model.addAttribute("currentPage", currentPage);

        switch (ordinarySortCriteria) {
            case "tickerUp":
                listOfStocksFound.sort(Comparators.tickerComparator);
                break;
            case "tickerDown":
                listOfStocksFound.sort(Comparators.tickerComparator.reversed());
                break;
            case "corpUp":
                listOfStocksFound.sort(Comparators.companyNameComparator);
                break;
            case "corpDown":
                listOfStocksFound.sort(Comparators.companyNameComparator.reversed());
                break;
            case "priceUp":
                listOfStocksFound.sort(Comparators.latestPriceComparator);
                break;
            case "priceDown":
                listOfStocksFound.sort(Comparators.latestPriceComparator.reversed());
                break;
            case "patentsUp":
                listOfStocksFound.sort(Comparators.patentsPortfolioComparator);
                break;
            case "patentsDown":
                listOfStocksFound.sort(Comparators.patentsPortfolioComparator.reversed());
                break;
        }

        model.addAttribute("listOfStocksFound", listOfStocksFound);
        paginatedListingService.setListOfItems(listOfStocksFound);

        //used in (fragments :: listingResults) in search.html
        //model.addAttribute("stocks", stocks);

        //used to display the form in search.html
        model.addAttribute("columns", columnChoices);
        model.addAttribute("title", "Found stock(s)");

        Page<Stock> stockPage = paginatedListingService.findPaginated(PageRequest.of(currentPage - 1, pageSize));
        model.addAttribute("stockPage", stockPage);
        int totalPages = stockPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            //model.addAttribute("pageNumbers", pageNumbers);
            List<String> reducedPagination = paginatedListingService.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }
        return "stocks/search";
        //return "redirect:/search/results";
    }

    /**
     * Display ALL stocks OR stocks relevant
     * to a specific tagId (with pagination)
     * <p>
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     * <p>
     * "listingResults" works with: stockPage
     * <p>
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     * <p>
     * This method returns at URL /stocks/index
     */
    @RequestMapping(method = RequestMethod.GET)
    public String displayStocks(
            @RequestParam(required = false) Integer tagId,
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size,
            @RequestParam("sortIcon") Optional<String> sortIcon) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);
        ordinarySortCriteria = sortIcon.orElse(ordinarySortCriteria);
        String iconsDestinationUrl = "/stocks/?sortIcon=";
        String paginationDestinationUrl = "/stocks/?size=";

        model.addAttribute("sortIcon", ordinarySortCriteria);
        model.addAttribute("baseColor", baseColor);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);
        //model.addAttribute("currentPage", currentPage);

        loadIP30PriceAndPatentsFootprint(model);
        if (tagId == null) { // display ALL stocks
            model.addAttribute("title", "All Stocks");
            listOfStocksFound = (List<Stock>) stockRepository.findAll();
            //model.addAttribute("stocks", stockRepository.findAll());
        } else { // display stocks for tagId
            Optional<Tag> result = tagRepository.findById(tagId);
            if (result.isEmpty()) {
                listOfStocksFound = new ArrayList<Stock>();//null;
                model.addAttribute("title", "Invalid Tag ID: " + tagId);

                return "stocks/index";
            } else { // there are stocks for that tag!
                Tag tag = result.get();
                model.addAttribute("title", "All Stocks for: " + tag.getName());
                listOfStocksFound = tag.getStocks();
                //model.addAttribute("stocks", tag.getStocks());
            }
        }

        switch (ordinarySortCriteria) {
            case "tickerUp":
                listOfStocksFound.sort(Comparators.tickerComparator);
                break;
            case "tickerDown":
                listOfStocksFound.sort(Comparators.tickerComparator.reversed());
                break;
            case "corpUp":
                listOfStocksFound.sort(Comparators.companyNameComparator);
                break;
            case "corpDown":
                listOfStocksFound.sort(Comparators.companyNameComparator.reversed());
                break;
            case "priceUp":
                listOfStocksFound.sort(Comparators.latestPriceComparator);
                break;
            case "priceDown":
                listOfStocksFound.sort(Comparators.latestPriceComparator.reversed());
                break;
            case "patentsUp":
                listOfStocksFound.sort(Comparators.patentsPortfolioComparator);
                break;
            case "patentsDown":
                listOfStocksFound.sort(Comparators.patentsPortfolioComparator.reversed());
                break;
        }

        paginatedListingService.setListOfItems(listOfStocksFound);
        Page<Stock> stockPage = paginatedListingService.findPaginated(PageRequest.of(currentPage - 1, pageSize));
        model.addAttribute("stockPage", stockPage);
        int totalPages = stockPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            List<String> reducedPagination = paginatedListingService.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }
        return "stocks/index";
    }

    /**
     * Display details of a stock
     * This method returns at URL /stocks/detail
     */
    @GetMapping("detail/{stockId}")
    public String displayStockDetails(
            @PathVariable Integer stockId,
            Model model) {
        Optional<Stock> result = stockRepository.findById(stockId);
        if (result.isEmpty()) {
            model.addAttribute("title", "Invalid Stock ID: " + stockId);
            //model.addAttribute(MESSAGE_KEY, "warning|No matching stock found in your portfolio");
        } else { // there are stocks for that stockId!
            Stock stock = result.get();
            model.addAttribute("title", "Summary of '" + stock.getTicker() + "'");
            model.addAttribute("stock", stock);
            model.addAttribute("exchangePlatforms", stockExchanges);
            if (stock.getStockDetails().isInPortfolio()) {
                model.addAttribute(MESSAGE_KEY, "info|The stock '" + stock.getTicker() + "' is currently in your portfolio");
            }
            loadAddtionalStockDataFromIexApi(stock.getTicker(), model);
        }
        return "stocks/detail";
    }

    /**
     * Display details of my portfolio
     * <p>
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     * <p>
     * "listingResults" works with: stockPage
     * <p>
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     * <p>
     * This method returns at URL /stocks/portfolio
     */
    @GetMapping("portfolio")
    public String displayPortfolioOfStocks(
            Model model,
            @RequestParam("page") Optional<Integer> page,
            @RequestParam("size") Optional<Integer> size,
            @RequestParam("sortIcon") Optional<String> sortIcon) {

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(numberOfItemsPerPage);
        inPortfolioSortCriteria = sortIcon.orElse(inPortfolioSortCriteria);
        String iconsDestinationUrl = "/stocks/portfolio/?sortIcon=";
        String paginationDestinationUrl = "/stocks/portfolio/?size=";

        model.addAttribute("baseColor", baseColor);
        model.addAttribute("sortIcon", inPortfolioSortCriteria);
        model.addAttribute("selectedColor", selectedColor);
        model.addAttribute("iconsDestinationUrl", iconsDestinationUrl);
        model.addAttribute("paginationDestinationUrl", paginationDestinationUrl);

        model.addAttribute("title", "My stocks portfolio");
        List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();
        List<Stock> portfolioList = new ArrayList<>();

        int aggregatedPatents = 0;
        double netWorth = 0.0;
        for (Stock stock : listOfStocks) {
            if (stock.getStockDetails().isInPortfolio()) {
                netWorth += stock.getStockDetails().getLatestPrice() * stock.getStockDetails().getNumberOfShares();
                aggregatedPatents += stock.getStockDetails().getTotalNumberOfPatents();
                portfolioList.add(stock);
            }
        }

        switch (inPortfolioSortCriteria) {
            case "tickerUp":
                portfolioList.sort(Comparators.tickerComparator);
                break;
            case "tickerDown":
                portfolioList.sort(Comparators.tickerComparator.reversed());
                break;
            case "corpUp":
                portfolioList.sort(Comparators.companyNameComparator);
                break;
            case "corpDown":
                portfolioList.sort(Comparators.companyNameComparator.reversed());
                break;
            case "priceUp":
                portfolioList.sort(Comparators.latestPriceComparator);
                break;
            case "priceDown":
                portfolioList.sort(Comparators.latestPriceComparator.reversed());
                break;
            case "patentsUp":
                portfolioList.sort(Comparators.patentsPortfolioComparator);
                break;
            case "patentsDown":
                portfolioList.sort(Comparators.patentsPortfolioComparator.reversed());
                break;
            case "sharesUp":
                portfolioList.sort(Comparators.numberOfSharesComparator);
                break;
            case "sharesDown":
                portfolioList.sort(Comparators.numberOfSharesComparator.reversed());
                break;
            case "percentInPortfolioUp":
                portfolioList.sort(Comparators.percentInPortfolioComparator);
                break;
            case "percentInPortfolioDown":
                portfolioList.sort(Comparators.percentInPortfolioComparator.reversed());
                break;
        }

        //model.addAttribute("portfolioList", portfolioList);
        model.addAttribute("aggregatedPatents", aggregatedPatents);
        model.addAttribute("netWorth", netWorth);

        model.addAttribute(MESSAGE_KEY, "info|Click on the number of shares to adjust it for each stock");
        paginatedListingService.setListOfItems(portfolioList);
        Page<Stock> stockPage = paginatedListingService.findPaginated(PageRequest.of(currentPage - 1, pageSize));
        model.addAttribute("stockPage", stockPage);
        int totalPages = stockPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            List<String> reducedPagination = paginatedListingService.paginating(currentPage, pageNumbers.size());
            model.addAttribute("pageNumbers", reducedPagination);//pageNumbers);
        }
        return "stocks/portfolio";
    }

    /**
     *
     * @param model
     * @param stockId
     * @return
     */
    @GetMapping("adjust-shares-portfolio/{stockId}")
    public String displayAdjustSharesInPortfolio(
            Model model,
            @PathVariable int stockId){
        Stock stock = stockRepository.findById(stockId).get();
        String ticker = stock.getTicker();
        model.addAttribute("title",
                "Edit number of shares for " + ticker);
        model.addAttribute("stock", stock);
        model.addAttribute(MESSAGE_KEY, "info|The" +
                    " number of shares must be a number between 1 and 1000");
        model.addAttribute("stockId", stockId);
        return "stocks/adjust-shares";
    }

    /**
     *
     * @param stockId
     * @param numberOfShares
     * @param redirectAttributes
     * @return
     */
    @PostMapping("adjust-shares-portfolio")
    public String processAdjustSharesInPortfolio(
            int stockId,
            int numberOfShares,
            RedirectAttributes redirectAttributes){
        Stock stock = stockRepository.findById(stockId).get();
        stock.getStockDetails().setNumberOfShares(numberOfShares);
        stockRepository.save(stock);
        redirectAttributes.addFlashAttribute(SECOND_MESSAGE_KEY, "success|The" +
                " number of shares has been updated successfully for " + stock.getTicker());
        return "redirect:/stocks/portfolio";
    }

    /**
     * Display details of a stock AFTER adding
     * it to the portfolio
     * This method returns at URL /stocks/detail
     */
    @GetMapping("add-to-portfolio/{stockId}")
    public String displayStockDetailsInPortfolio(
            @PathVariable Integer stockId,
            Model model) {
        Optional<Stock> result = stockRepository.findById(stockId);
        if (result.isEmpty()) {
            model.addAttribute("title", "Invalid Stock ID: " + stockId);
        } else { // there are stocks for that stockId!
            Stock stock = result.get();
            stock.getStockDetails().setInPortfolio(true);
            stockRepository.save(stock);
            model.addAttribute(MESSAGE_KEY, "success|The stock '" + stock.getTicker() + "' has been added to your portfolio");
            model.addAttribute("title", "Summary of '" + stock.getTicker() + "'");
            model.addAttribute("stock", stock);
            model.addAttribute("exchangePlatforms", stockExchanges);

            loadAddtionalStockDataFromIexApi(stock.getTicker(), model);
        }
        return "stocks/detail";
    }

    /**
     * This method retrieves current data from remote IEX API
     *
     * @param ticker
     * @param model
     */
    void loadAddtionalStockDataFromIexApi(String ticker, Model model) {
        // /stock/{symbol}/company
        String iexStockUrlString = BASE_URL_LIVE_IEX //BASE_URL_SANDBOX_IEX
                + "/stable/stock/" + ticker + "/company?token=" + IEX_PUBLIC_TOKEN;//IEX_TEST_TOKEN_TPK;

        // /stock/{symbol}/logo
        String iexStockLogoUrlString = BASE_URL_LIVE_IEX //BASE_URL_SANDBOX_IEX
                + "/stable/stock/" + ticker + "/logo?token=" + IEX_PUBLIC_TOKEN;//IEX_TEST_TOKEN_TPK;

        RestTemplate iexRestTemplate = new RestTemplate();
        String iexApiQueryResult = iexRestTemplate.getForObject(iexStockUrlString, String.class);
        String iexApiLogoQueryResult = iexRestTemplate.getForObject(iexStockLogoUrlString, String.class);

        try {
            Object iexObj = new JSONParser().parse(iexApiQueryResult);
            JSONObject iexJo = (JSONObject) iexObj; //typecasting iexObj to JSONObject
            model.addAttribute("website", iexJo.get("website"));
            model.addAttribute("description", iexJo.get("description"));
            model.addAttribute("industry", iexJo.get("industry"));
            model.addAttribute("CEO", iexJo.get("CEO"));
            model.addAttribute("sector", iexJo.get("sector"));
            model.addAttribute("employees", iexJo.get("employees"));
            model.addAttribute("address", iexJo.get("address"));
            model.addAttribute("state", iexJo.get("state"));
            model.addAttribute("city", iexJo.get("city"));
            model.addAttribute("zip", iexJo.get("zip"));
            model.addAttribute("country", iexJo.get("country"));
            model.addAttribute("phone", iexJo.get("phone"));

            iexObj = new JSONParser().parse(iexApiLogoQueryResult);
            iexJo = (JSONObject) iexObj; //typecasting iexObj to JSONObject
            model.addAttribute("logoUrl", iexJo.get("url"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Display details of a stock AFTER
     * removing a stock from portfolio
     * This method returns at URL /stocks/detail
     */
    @GetMapping("remove-from-portfolio/{stockId}")
    public String displayStockDetailsRemovedFromPortfolio(
            @PathVariable Integer stockId,
            Model model) {
        Optional<Stock> result = stockRepository.findById(stockId);
        if (result.isEmpty()) {
            model.addAttribute("title", "Invalid Stock ID: " + stockId);
        } else { // there are stocks for that stockId!
            Stock stock = result.get();
            stock.getStockDetails().setNumberOfShares(0);
            stock.getStockDetails().setInPortfolio(false);
            stockRepository.save(stock);
            model.addAttribute(MESSAGE_KEY, "success|The stock '" + stock.getTicker() + "' has been removed from your portfolio. " +
                    "All shares previously set to this stock have been wiped out.");
            model.addAttribute("title", "Summary of '" + stock.getTicker() + "'");
            model.addAttribute("stock", stock);
            model.addAttribute("exchangePlatforms", stockExchanges);

            loadAddtionalStockDataFromIexApi(stock.getTicker(), model);
        }
        return "stocks/detail";
    }

    /**
     * add tag to a stock
     * responds to /stocks/add-tag?stockId=13
     *
     * @param stockId
     * @param model
     * @return
     */
    @GetMapping("add-tag/{stockId}")
    public String displayAddTagForm(
            @PathVariable Integer stockId,
            Model model) {
        Optional<Stock> result = stockRepository.findById(stockId);
        Stock stock = result.get();
        model.addAttribute("title", "Add Tag to: " + stock.getTicker());
        model.addAttribute("tags", tagRepository.findAll());
        //model.addAttribute("stock", stock);
        StockTagDTO stockTagDTO = new StockTagDTO();
        stockTagDTO.setStock(stock);
        model.addAttribute("stockTag", stockTagDTO);
        return "stocks/add-tag";
    }

    /**
     * Add tag to a stock
     * responds to /stocks/add-tag?stockId=13
     *
     * @param stockTag
     * @param errors
     * @param redirectAttributes
     * @return
     */
    @PostMapping("add-tag")
    public String processAddTagForm(
            @ModelAttribute @Valid StockTagDTO stockTag,
            Errors errors,
            RedirectAttributes redirectAttributes) {
        if (!errors.hasErrors()) {
            Stock stock = stockTag.getStock();
            Tag tag = stockTag.getTag();
            if (!stock.getTags().contains(tag)) {
                stock.addTag(tag);
                stockRepository.save(stock);
                redirectAttributes.addFlashAttribute(SECOND_MESSAGE_KEY, "success|New investment field " + tag.getDisplayName() + " added to stock '" +
                        stock.getTicker() + "'");
            } else {
                redirectAttributes.addFlashAttribute(SECOND_MESSAGE_KEY, "info|Investment field " + tag.getDisplayName() + " is already set to stock '" +
                        stock.getTicker() + "'");
            }
            return "redirect:detail/" + stock.getId();
        }
        return "redirect:add-tag/" + stockTag.getStock().getId();
    }

    /**
     * remove tag from a stock
     * responds to /stocks/remove-tag?stockId=13
     *
     * @param stockId
     * @param model
     * @return
     */
    @GetMapping("remove-tag/{stockId}")
    public String displayRemoveTagForm(
            @PathVariable Integer stockId,
            Model model) {
        Optional<Stock> result = stockRepository.findById(stockId);
        Stock stock = result.get();
        model.addAttribute("title", "Remove Tag from: " + stock.getTicker());
        model.addAttribute("tags", stock.getTags());//tagRepository.findAll());
        model.addAttribute("stockId", stockId);
//        StockTagDTO stockTagDTO = new StockTagDTO();
//        stockTagDTO.setStock(stock);
//        model.addAttribute("stockTag", stockTagDTO);
        return "stocks/remove-tag";
    }

    /**
     * remove tag from a stock
     * responds to /stocks/remove-tag?stockId=13
     *
     * @param redirectAttributes
     * @return
     */
    @PostMapping("remove-tag")
    public String processRemoveTagForm(
            int stockId,
            @RequestParam(required = false) int[] tagIds,
            RedirectAttributes redirectAttributes) {
        Optional<Stock> result = stockRepository.findById(stockId);
        Stock stock = result.get();
        String messageString = "";
        if (tagIds != null) {
            for (int tagId : tagIds) {
                Tag tag = tagRepository.findById(tagId).get();
                if (stock.getTags().contains(tag)) {
                    stock.getTags().remove(tag);
                    messageString += tag.getDisplayName() + " ";
                } // else {} No reason why that tag would not be in there.
            }
        }
        messageString = (messageString=="")?"None":messageString;
        redirectAttributes.addFlashAttribute(SECOND_MESSAGE_KEY, "success|The following investment(s) field(s) " +
                "got removed from stock " + stock.getTicker() + ": " + messageString);
        stockRepository.save(stock);
        return "redirect:detail/" + stock.getId();
    }

    /*
    @GetMapping("create")
    public String displayCreateStockForm(Model model) {
        model.addAttribute("title", "Create Stock");
        model.addAttribute(new Stock());
        model.addAttribute("categories", stockCategoryRepository.findAll());
        return "stocks/create";
    }

    @PostMapping("create")
    public String processCreateStockForm(@ModelAttribute @Valid Stock newStock,
                                         Errors errors, Model model) {
        if (errors.hasErrors()) {
            model.addAttribute("title", "Create Stock");
            return "stocks/create";
        }

        stockRepository.save(newStock);
        return "redirect:";
    }*/

    /*
    @GetMapping("delete")
    public String displayDeleteStockForm(Model model) {
        model.addAttribute("title", "Delete Stocks");
        model.addAttribute("stocks", stockRepository.findAll());
        return "stocks/delete";
    }

    @PostMapping("delete")
    public String processDeleteStocksForm(@RequestParam(required = false) int[] stockIds) {
        if (stockIds != null) {
            for (int id : stockIds) {
                stockRepository.deleteById(id);
            }
        }
        return "redirect:";
    }*/

    /**
     * CALL USPTO API to refresh data
     */
    void loadUsptoAPI() {
        List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();

        //Preparatory settings For USPTO API call
        final String o = "{\"page\":0,\"per_page\":1}";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("LLL dd, yyyy");
        final String dateForUsptoApiUpdate = LocalDate.now().format(formatter);
        String usptoStockUrlString = "";
        String usptoApiQueryResult = "";
        String stock_q = ""; // contains the usptoApi key

        int i = 0; //for display of API update progress
        for (Stock stock : listOfStocks) {
            //GET USPTO Data while using usptoId
            usptoStockUrlString = BASE_URL_USPTO + "api/patents/query?q={stock_q}&f=[\"assignee_type\"]&o={o}";
            stock_q = "{\"assignee_id\":\"" + stock.getUsptoId() + "\"}";
            RestTemplate usptoRestTemplate = new RestTemplate();
            usptoApiQueryResult = usptoRestTemplate.getForObject(usptoStockUrlString, String.class, stock_q, o);

            // parsing usptoApiQueryResult
            try {
                Object usptoObj = new JSONParser().parse(usptoApiQueryResult);
                JSONObject usptoJo = (JSONObject) usptoObj; //typecasting usptoObj to JSONObject
                long total_patent_count = (long) usptoJo.get("total_patent_count");
                stock.getStockDetails().setTotalNumberOfPatents(total_patent_count);
                stock.getStockDetails().setLastUsptoApiUpdate(dateForUsptoApiUpdate);

                //Save USPTO API updates: lastUsptoApiUpdate, totalNumberOfPatents
                stock.getStockDetails().setTotalNumberOfPatents(total_patent_count);
                stock.getStockDetails().setLastUsptoApiUpdate(dateForUsptoApiUpdate);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            //Save obtained data to update local repositories
            stockRepository.save(stock);
//            System.out.println("stock " + i++ + " --> " + stock.getStockDetails().getTotalNumberOfPatents());
        }
        System.out.println("Completed USPTO API calls");
    }

    /**
     * CALL IEX API to refresh data
     */
    void loadIexApi() {
        List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();
        //Preparatory settings For IEX API call
        String iexStockUrlString;
        String iexApiQueryResult = "";

        //BASE_URL_SANDBOX_IEX_SSE
        //IEX_SANDBOX_PUBLIC_TOKEN_TSK
        //BASE_URL_SANDBOX_IEX_SSE
        //IEX_SANDBOX_PUBLIC_TOKEN_TSK
        //IEX_SANDBOX_SECRET_TOKEN

//        String result = restTemplate.getForObject(uri, String.class);
//        ExponentialBackOff backOff = new ExponentialBackOff(100, 1.5);
//        backOff.setMaxInterval(5 * 1000L);
//        backOff.setMaxElapsedTime(50 * 1000L);

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 1.1);
        backOff.setMaxElapsedTime(50000L);
        BackOffExecution execution = backOff.start();

        int i = 0; //for display of API update progress
        int k = 0;
        int t = 0;
        int delta = 0;
        int oldDelta = 0;
        nextStock:
        for (Stock stock : listOfStocks) {
            k += 1;
            //System.out.println("Visiting stock " + k);
            //if (stock.getStockDetails().getLatestPrice() != 0.0) continue nextStock;
            //GET IEX Data while using ticker
            //https://sandbox.iexapis.com/stable/stock/twtr/quote?token=Tsk_acfbaf3e2b4444378ac1b46b2570b2da&filter=latestTime,latestPrice

            //Fake price data
//            iexStockUrlString =BASE_URL_SANDBOX_IEX
//                            + "/stable/stock/" + stock.getTicker() + "/quote?token="
//                            + IEX_SANDBOX_PUBLIC_TOKEN_TSK + "&filter=latestTime,latestPrice";

            //Real price data
            iexStockUrlString = BASE_URL_LIVE_IEX + "/stable/stock/" + stock.getTicker() + "/quote?token="
                    + IEX_PUBLIC_TOKEN + "&filter=latestTime,latestPrice";

            RestTemplate iexRestTemplate = new RestTemplate();

            long waitTime = 0;

            //because too much time elapsed while trying
            stop:
            while (waitTime != BackOffExecution.STOP) {

                again:
                while (true) {
                    try {
                        iexApiQueryResult = iexRestTemplate.getForObject(iexStockUrlString, String.class);
                        //System.out.println("iexApiQueryResult = " + iexApiQueryResult);
                        if (iexApiQueryResult.charAt(0) == '{') {//read was successful
                            //System.out.println("iexApiQueryResult = " + iexApiQueryResult);
                            // parsing iexApiQueryResult
                            try {
                                Object iexObj = new JSONParser().parse(iexApiQueryResult);
                                JSONObject iexJo = (JSONObject) iexObj; //typecasting iexObj to JSONObject
                                String latestTime = (String) iexJo.get("latestTime");

                                double latestPrice = 0.0;
                                if (iexJo.get("latestPrice") instanceof Double) {
                                    //System.out.println("This is a Double");
                                    latestPrice = Double.valueOf((Double) iexJo.get("latestPrice"));
                                } else if (iexJo.get("latestPrice") instanceof String) {
                                    latestPrice = Double.valueOf((String) iexJo.get("latestPrice"));
                                    //System.out.println("This is a String");
                                } else if (iexJo.get("latestPrice") instanceof Float) {
                                    //System.out.println("This is a Float");
                                    latestPrice = Double.valueOf(((Float) iexJo.get("latestPrice")).floatValue());
                                } else if (iexJo.get("latestPrice") instanceof Long) {
                                    //System.out.println("This is a Long");
                                    latestPrice = Double.valueOf(((Long) iexJo.get("latestPrice")).longValue());
                                } else {
                                    latestPrice = Double.valueOf((String) iexJo.get("latestPrice"));
                                    System.out.println("1: latest price: " + iexJo.get("latestPrice")
                                            + " | This is a(n) " + iexJo.get("latestPrice").getClass().getSimpleName());
                                }
                                //latestPrice = Double.valueOf((String) iexJo.get("latestPrice"));
                                //System.out.println("latestTime " + latestTime + " | latestPrice " + latestPrice);

                                //Save IEX API updates: latestTime, latestPrice
                                stock.getStockDetails().setLastTradeTime(latestTime);
                                stock.getStockDetails().setLatestPrice(latestPrice);
                                //Save obtained data to update local repositories
                                stockRepository.save(stock);
                                i++;

                                delta = k - i;
                                if (oldDelta != delta) {
                                    System.out.println("stock with k = " + k + " | i = " + i +
                                            " --> " + stock.getStockDetails().getLatestPrice());
                                    oldDelta = delta;
                                } else {

                                }

                                continue nextStock;
                            } catch (ParseException e) {
                                e.printStackTrace();
                            } catch (NumberFormatException numberFormatException) {
                                numberFormatException.printStackTrace();
                            }
                        } else {
                            System.out.println("How come we are Here???");
                        }
                    } catch (Throwable ignored) {
                        waitTime = execution.nextBackOff();
                        if (waitTime != BackOffExecution.STOP) {
                            //System.out.println("Request just failed. Backing off for " + waitTime + "ms");
                            try {
                                Thread.sleep(waitTime);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break again;
                        } else {
                            t++;

//                            System.out.println("Not giving up on this stock " + k);
                            execution = backOff.start();
                            waitTime = 0;

                            break again;
                        }
                    }
                } //end while (true) again:

            }
        }
//        System.out.println("Read stocks i = " + i);
        //System.out.println("All stocks k = " + k);
//        System.out.println("Problematic stocks t = " + t);
        System.out.println("Completed IEX API calls");
    }

    /**
     * Display details of my portfolio
     * <p>
     * "headerStocksRow" works with:
     * sortIcon (Optional), iconsDestinationUrl, selectedColor, baseColor
     * <p>
     * "listingResults" works with: stockPage
     * <p>
     * "listingResultsPagination" works with:
     * stockPage, pageNumbers, currentPage, paginationDestinationUrl
     * <p>
     * This method returns at URL /stocks/portfolio
     */
    @GetMapping("callAPIs")
    public String displayStocksAfterCallingAPIs() {
        loadUsptoAPI();
        loadIexApi();
        System.out.println("Completed API calls");
        return "redirect:/stocks";
    }

    @GetMapping("IP30")
    public String displayIP30(Model model) {
        int size = 30;
        List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();
        listOfStocks.sort(Comparators.patentsPortfolioComparator.reversed());
        List<Stock> IP30List = new ArrayList<>(size);
        IP30List.addAll(listOfStocks.subList(0, size));
        model.addAttribute("title", "IP 30");
        model.addAttribute("IP30List", IP30List);
        loadIP30PriceAndPatentsFootprint(model);
        model.addAttribute("exchangePlatforms", stockExchanges);
        return "stocks/IP30";
    }

    void loadIP30PriceAndPatentsFootprint(Model model) {
        int size = 30;
        List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();
        listOfStocks.sort(Comparators.patentsPortfolioComparator.reversed());
        List<Stock> IP30List = new ArrayList<>(size);
        IP30List.addAll(listOfStocks.subList(0, size));
        long aggregatedPatents = 0L;
        double numerator = 0.0;
        for (Stock stock : IP30List) {
            aggregatedPatents += stock.getStockDetails().getTotalNumberOfPatents();
            numerator += stock.getStockDetails().getTotalNumberOfPatents() * stock.getStockDetails().getLatestPrice();
        }
        double weightedPrice = numerator / aggregatedPatents;
        model.addAttribute("aggregatedPatents", aggregatedPatents);
        model.addAttribute("weightedPrice", weightedPrice);
    }

}
