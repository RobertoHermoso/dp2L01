/*
 * CustomerController.java
 * 
 * Copyright (C) 2018 Universidad de Sevilla
 * 
 * The use of this project is hereby constrained to the conditions of the
 * TDG Licence, a copy of which you may download from
 * http://www.tdg-seville.info/License.html
 */

package controllers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import services.ConfigurationService;
import services.MemberService;
import services.RequestService;
import domain.Finder;
import domain.Member;
import domain.Procession;
import domain.Request;
import domain.Status;

@Controller
@RequestMapping("/request/member/")
public class RequestMemberController extends AbstractController {

	@Autowired
	private RequestService			requestService;
	@Autowired
	private MemberService			memberService;
	@Autowired
	private ConfigurationService	configurationService;


	// Constructors -----------------------------------------------------------

	public RequestMemberController() {
		super();
	}

	// List ---------------------------------------------------------------		

	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ModelAndView requestsList() {
		ModelAndView result;

		Member loggedMember = this.memberService.securityAndMember();
		Collection<Request> requests = this.requestService.getRequestsByMember(loggedMember);

		result = new ModelAndView("member/requests");

		result.addObject("requests", requests);

		return result;
	}

	@RequestMapping(value = "/filter", method = RequestMethod.POST, params = "refresh")
	public ModelAndView requestsFilter(@Valid String fselect) {
		ModelAndView result;

		if (fselect.equals("ALL"))
			result = new ModelAndView("redirect:list.do");
		else {

			Status status = Status.APPROVED;
			if (fselect.equals("PENDING"))
				status = Status.PENDING;
			else if (fselect.equals("REJECTED"))
				status = Status.REJECTED;

			Member loggedMember = this.memberService.securityAndMember();
			Collection<Request> requests = this.requestService.getRequestsByMemberAndStatus(loggedMember, status);

			result = new ModelAndView("member/requests");

			result.addObject("requests", requests);
		}

		return result;
	}

	@RequestMapping(value = "/delete", method = RequestMethod.GET)
	public ModelAndView requestDelete(@RequestParam int requestId) {
		ModelAndView result;

		Member loggedMember = this.memberService.securityAndMember();

		Boolean flag;

		try {
			this.requestService.deleteRequestAsMember(loggedMember, requestId);
			Collection<Request> requests = this.requestService.getRequestsByMember(loggedMember);

			result = new ModelAndView("member/requests");

			flag = true;

			result.addObject("requests", requests);
			result.addObject("flag", flag);
		} catch (Throwable oops) {
			Collection<Request> requests = this.requestService.getRequestsByMember(loggedMember);

			result = new ModelAndView("member/requests");

			flag = false;

			result.addObject("requests", requests);
			result.addObject("flag", flag);
		}

		return result;
	}

	@RequestMapping(value = "/create", method = RequestMethod.GET
	//RequestMethod.POST, params = "saveRequest"
	)
	public ModelAndView requestCreate(@RequestParam int processionId) {
		ModelAndView result;

		Member member = this.memberService.securityAndMember();

		Boolean flag;

		Hibernate.initialize(member.getFinder());
		Finder finder = member.getFinder();

		//Current Date
		Date currentDate = new Date();

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(currentDate);
		Integer currentDay = calendar.get(Calendar.DATE);
		Integer currentMonth = calendar.get(Calendar.MONTH);
		Integer currentYear = calendar.get(Calendar.YEAR);
		Integer currentHour = calendar.get(Calendar.HOUR);

		//LastEdit Finder
		Date lasEdit = finder.getLastEdit();
		calendar.setTime(lasEdit);
		Integer lastEditDay = calendar.get(Calendar.DATE);
		Integer lastEditMonth = calendar.get(Calendar.MONTH);
		Integer lastEditYear = calendar.get(Calendar.YEAR);
		Integer lastEditHour = calendar.get(Calendar.HOUR);

		Integer time = this.configurationService.getConfiguration().getTimeFinder();

		List<Procession> processions = new ArrayList<>();
		Hibernate.initialize(finder.getProcessions());
		List<Procession> finderProcessions = finder.getProcessions();

		if (currentDay.equals(lastEditDay) && currentMonth.equals(lastEditMonth) && currentYear.equals(lastEditYear) && lastEditHour < (currentHour + time)) {
			Integer numFinderResult = this.configurationService.getConfiguration().getFinderResult();

			if (finderProcessions.size() > numFinderResult)
				for (int i = 0; i < numFinderResult; i++)
					processions.add(finderProcessions.get(i));
			else
				processions = finderProcessions;
		}

		if (this.requestService.canRequest(member, processionId))
			try {
				this.requestService.createRequestAsMember(member, processionId);

				result = new ModelAndView("member/finderResult");

				flag = true;

				result.addObject("flag", flag);
				result.addObject("processions", processions);
				result.addObject("member", member);
			} catch (Throwable oops) {
				result = new ModelAndView("member/finderResult");

				flag = false;

				result.addObject("flag", flag);
				result.addObject("processions", processions);
				result.addObject("member", member);
			}
		else {
			result = new ModelAndView("member/finderResult");

			flag = false;

			result.addObject("flag", flag);
			result.addObject("processions", processions);
			result.addObject("member", member);
		}

		return result;
	}
}
