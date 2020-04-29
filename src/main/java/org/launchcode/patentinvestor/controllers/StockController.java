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
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
        } else { // there are stocks for that stockId!
            Stock stock = result.get();
            model.addAttribute("title", "Summary of '" + stock.getTicker() + "'");
            model.addAttribute("stock", stock);
            model.addAttribute("exchangePlatforms", stockExchanges);
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

        getTwoApiData();

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

        model.addAttribute("title", "My Stocks Portfolio");
        List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();
        List<Stock> portfolioList = new ArrayList<>();

        int aggregatedPatents = 0;
        double netWorth = 0.0;
        for (Stock stock : listOfStocks) {
            if (stock.getStockDetails().isInPortfolio()) {
                netWorth += stock.getStockDetails().getLatestPrice();
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
     * Display details of a stock
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
        }
        return "stocks/detail";
    }

    // add tag to a specific stock : responds to /stocks/add-tag?stockId=13
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

    // responds to /stocks/add-tag?stockId=13
    @PostMapping("add-tag")
    public String processAddTagForm(
            @ModelAttribute @Valid StockTagDTO stockTag,
            Errors errors,
            RedirectAttributes redirectAttributes) {
        if (!errors.hasErrors()) {
            //System.out.println("HERE");
            Stock stock = stockTag.getStock();
            Tag tag = stockTag.getTag();
            if (!stock.getTags().contains(tag)) {
                stock.addTag(tag);
                stockRepository.save(stock);
                redirectAttributes.addFlashAttribute(MESSAGE_KEY, "success|New investment field " + tag.getDisplayName() + " added to stock '" +
                        stock.getTicker() + "'");
            } else {
                redirectAttributes.addFlashAttribute(MESSAGE_KEY, "info|Investment field " + tag.getDisplayName() + " is already set to stock '" +
                        stock.getTicker() + "'");
            }
            return "redirect:detail/" + stock.getId();
        }
        return "redirect:add-tag/" + stockTag.getStock().getId();
    }

    /**
     * Both "BASE_URL" end with '/'
     */
    private void getTwoApiData() {

//        class InnerUsptoObject {
//            String patents;
//            int count;
//            int  total_patent_count;
////            void innerMethod() {
////                System.out.println("inside innerMethod");
////            }
//        }

        //List<Stock> listOfStocks = (List<Stock>) stockRepository.findAll();
        //String urlString = "https://www.patentsview.org/api/patents/query?q={\"assignee_id\":\"org_2iJoxmpyjABCOAVmBomhr\"}&f=[\"assignee_type\"]&o={\"page\":0,\"per_page\":1}";//BASE_URL_USPTO;
        String q = "{\"assignee_id\":\"org_2iJoxmpyjBCOAVmBomhr\"}";
        //String f = "[\"assignee_type\"]";
        String o = "{\"page\":0,\"per_page\":1}";
        //final String uri = urlString;
        String urlString = "https://www.patentsview.org/api/patents/query?q={q}&f=[\"assignee_type\"]&o={o}";//BASE_URL_USPTO;
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.getForObject(urlString, String.class, q, o);
        //System.out.println(result);
        // parsing result
        try {
            Object obj = new JSONParser().parse(result);

             //typecasting obj to JSONObject
            JSONObject jo = (JSONObject) obj;
            long total_patent_count = (long) jo.get("total_patent_count");
             System.out.println(total_patent_count);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        //System.out.println(result);

//        final String uri = BASE_URL_LIVE_IEX;//BASE_URL_SANDBOX_IEX
//        //IEX_SANDBOX_PUBLIC_TOKEN
//        //IEX_SANDBOX_SECRET_TOKEN
//        RestTemplate restTemplate = new RestTemplate();
//        String result = restTemplate.getForObject(uri, String.class);
//        System.out.println(result);
//        //IEX_TEST_TOKEN
//        //IEX_PUBLIC_TOKEN
//        //IEX_SECRET_TOKEN

//        for (Stock stock : listOfStocks) {
//            //GET USPTO Data while using usptoId
//            //in stock.stockDetails: lastUsptoApiUpdate, totalNumberOfPatents
//            String usptoId = stock.getUsptoId();
//            int totalNumberOfPatents = 0;
//            String lastUsptoApiUpdate = "";
//            stock.getStockDetails().setTotalNumberOfPatents(totalNumberOfPatents);
//            stock.getStockDetails().setLastUsptoApiUpdate(lastUsptoApiUpdate);
//
//            //GET IEX Data while using usptoId :
//            //in stock.stockDetails: lastTradeTime, latestPrice
//            String iexId = stock.getIexId();
//            String lastTradeTime ="";
//            double latestPrice = 0.0;
//            stock.getStockDetails().setLastTradeTime(lastTradeTime);
//            stock.getStockDetails().setLatestPrice(latestPrice);
//
//            //Save obtained data to update local repositories
//            stockRepository.save(stock);
//        }

    }

}
