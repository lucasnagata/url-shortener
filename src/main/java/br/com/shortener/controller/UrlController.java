package br.com.shortener.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.shortener.domain.ShortUrl;
import br.com.shortener.domain.Url;
import br.com.shortener.dto.UrlDTO;
import br.com.shortener.service.UrlService;

@Controller
public class UrlController {

	private final UrlService urlService;

	@Autowired
	public UrlController(UrlService urlService) {
		this.urlService = urlService;
	}

	@RequestMapping("/url")
	public ModelAndView test() {

		Url url = new Url();
		System.out.println(url.getId());
		return new ModelAndView("/url");
	}
	
	@RequestMapping(path = "/url/{urlToCheck}/**", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
	public @ResponseBody UrlDTO showShortenedUrl(HttpServletRequest request,
			@PathVariable(name = "urlToCheck", required = true) String urlToCheck) {
		
		Optional<Url> optionalUrl;		
		
		String requestURL = request.getRequestURL().toString();
		
		String[] splittedUrl = requestURL.split("/url/" + urlToCheck + "/");
		
		if(splittedUrl.length > 1) {
			optionalUrl = urlService.getByUrl(urlToCheck + splittedUrl[1]);
		} else {
			optionalUrl = urlService.getByUrl(urlToCheck);
		}		
		
		if (optionalUrl.isPresent()) {			
			return UrlDTO.from(optionalUrl.get());
		}
		
		Url url = new Url();
		
		if(splittedUrl.length > 1) {
			url.setUrl(urlToCheck + "/" + splittedUrl[1]);
		} else {
			url.setUrl(urlToCheck);
		}		
		
		url.setUrlUsage(Long.valueOf("0"));
		url = urlService.save(url);
		url.setShortened("shrt.io/" + ShortUrl.encode(url.getId().intValue()));
		urlService.save(url);
		
		return UrlDTO.from(url);
	}

	@RequestMapping(path = "/url/shrt.io/{shortened}", method = RequestMethod.GET)
	public ModelAndView redirect(@PathVariable(name = "shortened", required = true) String shortened,
			RedirectAttributes atributos) {
		ModelAndView mv = new ModelAndView("/url");

		try {

			Optional<Url> optionaShortenedUrl = urlService.getByShortened("shrt.io/" + shortened);

			if (optionaShortenedUrl.isPresent()) {
				optionaShortenedUrl.get().setUrlUsage(optionaShortenedUrl.get().getUrlUsage() + 1);
				urlService.save(optionaShortenedUrl.get());
				return new ModelAndView("redirect:http://" + optionaShortenedUrl.get().getUrl());
			}
		} catch (Exception erro) {
			erro.printStackTrace();
			atributos.addFlashAttribute("message", "error");
		}

		return mv;
	}
	
	@RequestMapping(path = "/url/stats", method = RequestMethod.GET, produces = "application/json; charset=UTF-8")
	public @ResponseBody List<UrlDTO> getStats() {
		List<UrlDTO> list = new ArrayList<>();
		
		for(Url url :urlService.getAll()) {
			list.add(UrlDTO.from(url));
		}
		
		return list;
	}
}
