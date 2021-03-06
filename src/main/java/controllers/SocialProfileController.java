
package controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import security.Authority;
import security.LoginService;
import security.UserAccount;
import services.ActorService;
import services.AdminService;
import services.BrotherhoodService;
import services.ConfigurationService;
import services.FloatService;
import services.MemberService;
import services.SocialProfileService;
import domain.Actor;
import domain.Admin;
import domain.Brotherhood;
import domain.Configuration;
import domain.Member;
import domain.SocialProfile;

@Controller
@RequestMapping("/authenticated")
public class SocialProfileController extends AbstractController {

	@Autowired
	private ActorService			actorService;

	@Autowired
	private SocialProfileService	socialProfileService;

	@Autowired
	private BrotherhoodService		brotherhoodService;
	@Autowired
	private AdminService			adminService;
	@Autowired
	private MemberService			memberService;

	@Autowired
	private FloatService			floatService;

	@Autowired
	private ConfigurationService	configurationService;


	//-------------------------------------------------------------------
	//---------------------------LIST BROTHERHOOD------------------------------------
	@RequestMapping(value = "/showProfile", method = RequestMethod.GET)
	public ModelAndView list() {
		ModelAndView result;
		Brotherhood broherhood = new Brotherhood();
		UserAccount userAccount;
		userAccount = LoginService.getPrincipal();
		Actor logguedActor = new Actor();
		List<SocialProfile> socialProfiles = new ArrayList<SocialProfile>();

		final List<Authority> authorities = (List<Authority>) userAccount.getAuthorities();

		if (authorities.get(0).toString().equals("BROTHERHOOD")) {
			broherhood = this.brotherhoodService.loggedBrotherhood();
			socialProfiles = broherhood.getSocialProfiles();
		} else {

			logguedActor = this.actorService.getActorByUsername(userAccount.getUsername());

			socialProfiles = logguedActor.getSocialProfiles();
		}

		result = new ModelAndView("authenticated/showProfile");
		result.addObject("socialProfiles", socialProfiles);
		result.addObject("actor", logguedActor);
		result.addObject("broherhood", broherhood);
		result.addObject("pictures", broherhood.getPictures());
		result.addObject("requestURI", "authenticated/showProfile.do");

		return result;
	}

	//---------------------------------------------------------------------
	//---------------------------CREATE BROTHERHOOD------------------------------------
	@RequestMapping(value = "/socialProfile/create", method = RequestMethod.GET)
	public ModelAndView create() {
		ModelAndView result;
		SocialProfile socialProfile;

		socialProfile = this.socialProfileService.create();
		result = this.createEditModelAndView(socialProfile);

		return result;
	}

	//---------------------------------------------------------------------
	//---------------------------EDIT BROTHERHOOD--------------------------------------
	@RequestMapping(value = "/socialProfile/edit", method = RequestMethod.GET)
	public ModelAndView edit(@RequestParam int socialProfileId) {

		ModelAndView result;
		SocialProfile socialProfile;

		Actor logged = this.actorService.getActorByUsername(LoginService.getPrincipal().getUsername());

		List<SocialProfile> socialProfiles = logged.getSocialProfiles();

		socialProfile = this.socialProfileService.findOne(socialProfileId);
		Assert.notNull(socialProfile);
		result = this.createEditModelAndView(socialProfile);

		if (!(socialProfiles.contains(socialProfile)))
			result = this.list();
		return result;
	}

	//---------------------------------------------------------------------
	//---------------------------SAVE --------------------------------------
	@RequestMapping(value = "/socialProfile/create", method = RequestMethod.POST, params = "save")
	public ModelAndView save(SocialProfile socialProfile, BindingResult binding) {
		ModelAndView result;
		Actor logguedActor = this.actorService.getActorByUsername(LoginService.getPrincipal().getUsername());

		socialProfile = this.socialProfileService.reconstruct(socialProfile, binding);

		if (binding.hasErrors())
			result = this.createEditModelAndView(socialProfile);
		else
			try {

				SocialProfile saved = this.socialProfileService.save(socialProfile);
				List<SocialProfile> socialProfiles = logguedActor.getSocialProfiles();

				if (socialProfiles.contains(socialProfile)) {
					socialProfiles.remove(saved);
					socialProfiles.add(saved);
				} else
					socialProfiles.add(saved);

				logguedActor.setSocialProfiles(socialProfiles);

				this.actorService.save(logguedActor);

				result = new ModelAndView("redirect:/authenticated/showProfile.do");
			} catch (Throwable oops) {
				result = this.createEditModelAndView(socialProfile, "socialProfile.commit.error");
			}
		return result;
	}
	//---------------------------------------------------------------------
	//---------------------------DELETE------------------------------------
	@RequestMapping(value = "/socialProfile/create", method = RequestMethod.POST, params = "delete")
	public ModelAndView delete(SocialProfile socialProfile, BindingResult binding) {

		ModelAndView result;

		socialProfile = this.socialProfileService.reconstruct(socialProfile, binding);

		try {

			this.socialProfileService.deleteSocialProfile(socialProfile);
			result = new ModelAndView("redirect:/authenticated/showProfile.do");

		} catch (Throwable oops) {
			result = this.createEditModelAndView(socialProfile, "socialProfile.commit.error");
		}
		return result;
	}

	//---------------------------------------------------------------------
	//---------------------------EDIT PERSONAL DATA------------------------
	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	public ModelAndView editPersonalData() {

		ModelAndView result;

		UserAccount userAccount;
		userAccount = LoginService.getPrincipal();

		List<Authority> authorities = (List<Authority>) userAccount.getAuthorities();

		if (authorities.get(0).toString().equals("ADMIN")) {
			Admin admin = this.adminService.loggedAdmin();
			Assert.notNull(admin);
			result = this.createEditModelAndView(admin);

		} else if (authorities.get(0).toString().equals("BROTHERHOOD")) {
			Brotherhood brotherhood = this.brotherhoodService.loggedBrotherhood();
			Assert.notNull(brotherhood);
			result = this.createEditModelAndView(brotherhood);
		} else {
			Member member = this.memberService.loggedMember();
			Assert.notNull(member);
			result = this.createEditModelAndView(member);
		}

		if (result == null)
			result = this.list();
		return result;
	}

	//---------------------------------------------------------------------
	//---------------------------SAVE PERSONAL DATA------------------------
	//Admin
	@RequestMapping(value = "/edit", method = RequestMethod.POST, params = "save")
	public ModelAndView saveAdmin(Admin admin, BindingResult binding) {
		ModelAndView result;

		admin = this.adminService.reconstruct(admin, binding);
		Configuration configuration = this.configurationService.getConfiguration();

		String prefix = configuration.getSpainTelephoneCode();

		if (binding.hasErrors())
			result = this.createEditModelAndView(admin);
		else
			try {
				if (admin.getPhoneNumber().matches("(\\+[0-9]{1,3})(\\([0-9]{1,3}\\))([0-9]{4,})$") || admin.getPhoneNumber().matches("(\\+[0-9]{1,3})([0-9]{4,})$"))
					this.adminService.save(admin);
				else if (admin.getPhoneNumber().matches("([0-9]{4,})$")) {
					admin.setPhoneNumber(prefix + admin.getPhoneNumber());
					this.adminService.save(admin);
				} else
					this.adminService.save(admin);
				result = new ModelAndView("redirect:/authenticated/showProfile.do");
			} catch (Throwable oops) {
				result = this.createEditModelAndView(admin, "socialProfile.commit.error");
			}
		return result;
	}

	//Member
	@RequestMapping(value = "/editMember", method = RequestMethod.POST, params = "save")
	public ModelAndView saveMember(Member member, BindingResult binding) {
		ModelAndView result;

		member = this.memberService.reconstruct(member, binding);
		Configuration configuration = this.configurationService.getConfiguration();

		String prefix = configuration.getSpainTelephoneCode();

		if (binding.hasErrors())
			result = this.createEditModelAndView(member);
		else
			try {
				if (member.getEmail().matches("[\\w.%-]+\\<[\\w.%-]+\\@+\\>|[\\w.%-]+")) {
					if (LocaleContextHolder.getLocale().getLanguage().toUpperCase().contains("ES")) {
						binding.addError(new FieldError("member", "email", member.getEmail(), false, null, null, "No sigue el patron ejemplo@dominio.asd o alias <ejemplo@dominio.asd>"));
						return this.createEditModelAndView(member);
					} else {
						binding.addError(new FieldError("member", "email", member.getEmail(), false, null, null, "Dont follow the pattern example@domain.asd or alias <example@domain.asd>"));
						return this.createEditModelAndView(member);
					}

				} else if (member.getPhoneNumber().matches("(\\+[0-9]{1,3})(\\([0-9]{1,3}\\))([0-9]{4,})$") || member.getPhoneNumber().matches("(\\+[0-9]{1,3})([0-9]{4,})$"))
					this.memberService.save(member);
				else if (member.getPhoneNumber().matches("([0-9]{4,})$")) {
					member.setPhoneNumber(prefix + member.getPhoneNumber());
					this.memberService.save(member);
				} else
					this.memberService.save(member);
				result = new ModelAndView("redirect:/authenticated/showProfile.do");
			} catch (Throwable oops) {
				result = this.createEditModelAndView(member, "socialProfile.commit.error");
			}
		return result;
	}

	//Brotherhood
	@RequestMapping(value = "/editBrotherhood", method = RequestMethod.POST, params = "save")
	public ModelAndView saveMember(Brotherhood brotherhood, BindingResult binding) {
		ModelAndView result;

		brotherhood = this.brotherhoodService.reconstructBrotherhood(brotherhood, binding);
		Configuration configuration = this.configurationService.getConfiguration();

		String prefix = configuration.getSpainTelephoneCode();

		if (binding.hasErrors())
			result = this.createEditModelAndView(brotherhood);
		else
			try {
				if (brotherhood.getEmail().matches("[\\w.%-]+\\<[\\w.%-]+\\@+\\>|[\\w.%-]+")) {
					if (LocaleContextHolder.getLocale().getLanguage().toUpperCase().contains("ES")) {
						binding.addError(new FieldError("brotherhood", "email", brotherhood.getEmail(), false, null, null, "No sigue el patron ejemplo@dominio.asd o alias <ejemplo@dominio.asd>"));
						return this.createEditModelAndView(brotherhood);
					} else {
						binding.addError(new FieldError("brotherhood", "email", brotherhood.getEmail(), false, null, null, "Dont follow the pattern example@domain.asd or alias <example@domain.asd>"));
						return this.createEditModelAndView(brotherhood);
					}

				} else if (brotherhood.getPhoneNumber().matches("(\\+[0-9]{1,3})(\\([0-9]{1,3}\\))([0-9]{4,})$") || brotherhood.getPhoneNumber().matches("(\\+[0-9]{1,3})([0-9]{4,})$"))
					this.brotherhoodService.save(brotherhood);
				else if (brotherhood.getPhoneNumber().matches("([0-9]{4,})$")) {
					brotherhood.setPhoneNumber(prefix + brotherhood.getPhoneNumber());
					this.brotherhoodService.save(brotherhood);
				} else
					this.brotherhoodService.save(brotherhood);
				result = new ModelAndView("redirect:/authenticated/showProfile.do");
			} catch (Throwable oops) {
				result = this.createEditModelAndView(brotherhood, "socialProfile.commit.error");
			}
		return result;
	}

	@RequestMapping(value = "/picture/list", method = RequestMethod.GET)
	public ModelAndView listPicturesBrotherhood(@RequestParam int brotherhoodId) {

		ModelAndView result;

		List<String> pictures;

		Brotherhood brotherhoood;

		brotherhoood = this.brotherhoodService.findOne(brotherhoodId);

		Assert.notNull(brotherhoood);
		Assert.isTrue(this.brotherhoodService.loggedBrotherhood().getId() == brotherhoodId);

		pictures = brotherhoood.getPictures();

		result = new ModelAndView("authenticated/picture/list");

		result.addObject("picturesBrotherhood", pictures);
		result.addObject("requestURI", "authenticated/picture/list.do");
		result.addObject("brotherhoodId", brotherhoodId);

		return result;
	}

	@RequestMapping(value = "/picture/create", method = RequestMethod.GET)
	public ModelAndView createPictures(@RequestParam int brotherhoodId) {
		ModelAndView result;

		result = new ModelAndView("authenticated/picture/create");
		result.addObject("brotherhoodId", brotherhoodId);

		return result;
	}

	@RequestMapping(value = "/picture/save", method = RequestMethod.POST, params = "save")
	public ModelAndView savePicture(String picture, int brotherhoodId) {
		ModelAndView result;
		Brotherhood brotherhood = new Brotherhood();
		brotherhood = this.brotherhoodService.findOne(brotherhoodId);

		Assert.isTrue(this.brotherhoodService.loggedBrotherhood().equals(brotherhood));

		try {
			if (picture.trim().isEmpty() || picture.trim().isEmpty() || !this.floatService.isUrl(picture)) {
				result = new ModelAndView("authenticated/picture/create");
				result.addObject("brotherhoodId", brotherhoodId);
			} else {
				this.brotherhoodService.addPicture(picture, brotherhood);
				result = new ModelAndView("redirect:list.do?brotherhoodId=" + brotherhoodId);

			}
		} catch (Throwable oops) {
			result = new ModelAndView("picture/brotherhood/createPicture");

		}

		return result;
	}
	//---------------------------------------------------------------------
	//---------------------------CREATEEDITMODELANDVIEW--------------------

	protected ModelAndView createEditModelAndView(SocialProfile socialProfile) {

		ModelAndView result;

		result = this.createEditModelAndView(socialProfile, null);

		return result;
	}

	protected ModelAndView createEditModelAndView(SocialProfile socialProfile, String messageCode) {

		ModelAndView result;

		result = new ModelAndView("authenticated/socialProfile/create");
		result.addObject("socialProfile", socialProfile);
		result.addObject("message", messageCode);

		return result;
	}

	//---------------------------------------------------------------------
	//-------------------CREATEEDITMODELANDVIEW ACTOR----------------------

	//Admin
	protected ModelAndView createEditModelAndView(Admin admin) {

		ModelAndView result;

		result = this.createEditModelAndView(admin, null);

		return result;
	}

	protected ModelAndView createEditModelAndView(Admin admin, String messageCode) {

		ModelAndView result;

		result = new ModelAndView("authenticated/edit");
		result.addObject("admin", admin);
		result.addObject("message", messageCode);

		return result;
	}

	//Brotherhood
	protected ModelAndView createEditModelAndView(Brotherhood brotherhood) {

		ModelAndView result;

		result = this.createEditModelAndView(brotherhood, null);

		return result;
	}

	protected ModelAndView createEditModelAndView(Brotherhood brotherhood, String messageCode) {

		ModelAndView result;

		result = new ModelAndView("authenticated/edit");
		result.addObject("brotherhood", brotherhood);
		result.addObject("message", messageCode);

		return result;
	}

	//Member
	protected ModelAndView createEditModelAndView(Member member) {

		ModelAndView result;

		result = this.createEditModelAndView(member, null);

		return result;
	}

	protected ModelAndView createEditModelAndView(Member member, String messageCode) {

		ModelAndView result;

		result = new ModelAndView("authenticated/edit");
		result.addObject("member", member);
		result.addObject("message", messageCode);

		return result;
	}
}
