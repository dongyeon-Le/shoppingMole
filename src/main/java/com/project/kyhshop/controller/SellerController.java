package com.project.kyhshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import com.project.kyhshop.dao.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class SellerController {
    @Autowired
    SellerDao sd;

    // 셀러 회원가입 페이지
    @GetMapping("/register/seller")
    public String sellerRegisterPage() {
        return "html/seller/register";
    }

    // 셀러 회원가입 액션
    @PostMapping("/register/seller/action")
    public String sellerRegisterAction(@RequestParam String id,
                                       @RequestParam String pw,
                                       @RequestParam String nm,
                                       @RequestParam String email,
                                       @RequestParam String phone_1,
                                       @RequestParam String phone_2,
                                       @RequestParam String phone_3,
                                       @RequestParam String address,
                                       @RequestParam String address_detail,
                                       @RequestParam String comp_nm,
                                       @RequestParam String biz_id_1,
                                       @RequestParam String biz_id_2,
                                       @RequestParam String biz_id_3,
                                       HttpSession session,
                                       Model model)
    {
        int dup = sd.sellerDupCheck(id);
        if (dup == 1) {
            model.addAttribute("link", "/login");
            model.addAttribute("msg", "아이디 중복 오류!!");
            return "/html/alert";
        }

        // 폰 번호 합치기 010-0000-0000
        String phone = String.format("%s-%s-%s", phone_1, phone_2, phone_3);

        // 사업자 등록번호 합치기
        String biz_id = String.format("%s-%s-%s", biz_id_1, biz_id_2, biz_id_3);

        sd.sellerRegister(id, pw, nm, email, phone, address, address_detail, comp_nm, biz_id);

        model.addAttribute("link", "/login");
        model.addAttribute("msg", "회원가입이 완료되었습니다.");
        return "/html/alert";
    }

    // 셀러 중복 확인
    @GetMapping("/seller/dupcheck")
    @ResponseBody
    public Map<String, Object> sellerDupCheck(@RequestParam String id) {
        int sel_dup = sd.sellerDupCheck(id);
        Map<String, Object> response = new HashMap<>();
        response.put("isDuplicate", sel_dup > 0);

        return response;
    }

    // 이메일 중복 확인
    @GetMapping("/seller/dupcheck/email")
    @ResponseBody
    public Map<String, Object> dupEmailCheck(@RequestParam String email) {
        int dup = sd.sellerDupEmailCheck(email);
        Map<String, Object> response = new HashMap<>();
        response.put("isDupEmail", dup > 0);

        return response;
    }

    // 셀러 로그인 액션
    @PostMapping("/login/seller/action")
    public String sellerLoginAction(@RequestParam String sellerId,
                                    @RequestParam String sellerPw,
                                    HttpSession session,
                                    Model model)
    {
        // 아이디, 비밀번호가 DB와 일치하면 정보(아이디, 이름)를 loginResult 변수에 가져오기
        Map<String, Object> loginResult = sd.sellerLogin(sellerId, sellerPw);

        // 로그인 정보가 있으면(아이디, 비밀번호 일치 O, del_fg == 0)
        if (loginResult != null && (int)loginResult.get("del_fg") == 0) {
            session.setAttribute("id", loginResult.get("id"));
            session.setAttribute("nm", loginResult.get("nm"));
            session.setAttribute("grade", loginResult.get("grade"));
            session.setAttribute("temp", "seller");
            return "redirect:/";
        } else if (loginResult != null && (int)loginResult.get("del_fg") == 1) {
            model.addAttribute("link", "/login/resign");
            model.addAttribute("msg", "탈퇴한 회원입니다. 탈퇴를 해제하시겠습니까?");
            return "/html/alert";
        } else {
            model.addAttribute("link", "/login");
            model.addAttribute("msg", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return "/html/alert";
        }
    }

    // 셀러 아이디 찾기 페이지
    @GetMapping("/find/seller/id")
    public String findSellerId() {
        return "html/seller/findid";
    }

    // 셀러 아이디 찾기 액션
    @GetMapping("/find/seller/id/action")
    @ResponseBody
    public Map<String, Object> findSellerIdAction(@RequestParam String nm,
                                                  @RequestParam String email)
    {
        String findId = sd.findSellerId(nm, email);

        Map<String, Object> response = new HashMap<>();
        response.put("id", findId);

        return response;
    }

    // 비밀번호 확인 페이지 (개인정보 수정을 위해)
    @GetMapping("my/seller/passcheck")
    public String sellerPassCheck(HttpSession session) {

        String id = (String)session.getAttribute("id");
        int ox = sd.sellerOX(id);

        // 셀러가 아니면 로그인 페이지로
        if (ox == 0) {
            return "redirect:/login";
        } 

        return "html/seller/passwordcheck";
    }

    // 셀러 회원수정 페이지
    @PostMapping("my/seller/profile/verify")
    public String userProfile(@RequestParam String id,
                              @RequestParam String pw,
                              HttpSession session,
                              Model model)
    {
        String sellerId = (String)session.getAttribute("id");
        int ox = sd.sellerOX(id);

        // 셀러가 아니면 로그인 페이지로
        if (ox == 0) {
            return "redirect:/login";
        } 
        // 로그인 되어 있으면
        if (sellerId != null) {
            int verifyCnt = sd.sellerVerifyPass(id, pw);  // 비밀번호 확인

            if (verifyCnt == 1) {                   // 비밀번호 확인 되면
                List<Map<String, Object>> sellerSelect = sd.selectSeller(sellerId);   // 셀러 정보 가져오기
                model.addAttribute("sellerSelect", sellerSelect);       // 셀러 정보 addAttribute

                return "html/seller/sellerchange";
            } else {                                // 비밀번호 확인 실패 시
                model.addAttribute("link", "/my/seller/passcheck");
                model.addAttribute("msg", "틀린 비밀번호 입니다.");
                return "/html/alert";
            }
        } else {
            return "/html/login";
        }
    }

    // 셀러 수정하기 액션
    @PostMapping("my/seller/profile/changeaction")
    @ResponseBody
    public String sellerChangeAction(@RequestParam String pw,
                                     @RequestParam String nm,
                                     @RequestParam String email,
                                     @RequestParam String phone1,
                                     @RequestParam String phone2,
                                     @RequestParam String phone3,
                                     @RequestParam String address,
                                     @RequestParam String addressDetail,
                                     @RequestParam String compNm,
                                     @RequestParam String bizId1,
                                     @RequestParam String bizId2,
                                     @RequestParam String bizId3,
                                     HttpSession session,
                                     Model model)
    {
        String id = (String)session.getAttribute("id");
        int ox = sd.sellerOX(id);

        // 셀러가 아니면 로그인 페이지로
        if (ox == 0) {
            return "redirect:/login";
        } 
        // 로그인 되어 있으면
        if (id != null) {
            String sellerId = (String)session.getAttribute("id");

            // 폰 번호 합치기 010-0000-0000
            String phone = String.format("%s-%s-%s", phone1, phone2, phone3);
            // 사업자 등록 번호 합치기
            String bizId = String.format("%s-%s-%s", bizId1, bizId2, bizId3);
            // 셀러 전 정보 중복확인
            int sellerSelect = sd.sellerSelect(sellerId, pw, email, phone, address, addressDetail, compNm, bizId);

            if (sellerSelect == 1) {
                return "<script>window.history.back(); alert('변경된 정보가 없습니다.');</script>";
            }

            sd.sellerProfileChange(sellerId, pw, nm, email, phone, addressDetail, address, compNm, bizId);
        } else {
            return "<script>window.close();</script>";
        }
        return "<script>window.close(); alert('회원정보가 수정되었습니다.');</script>";
    }

    // 셀러 삭제하기 액션
    @PostMapping("my/seller/profile/deleteaction")
    @ResponseBody
    public String sellerDeleteAction(HttpSession session,
                                     Model model)
    {
        String id = (String)session.getAttribute("id");
        int ox = sd.sellerOX(id);

        // 셀러가 아니면 로그인 페이지로
        if (ox == 0) {
            return "redirect:/login";
        } 
        // 로그인 되어 있으면
        if (id != null) {
            if (sd.sellerPageCnt(id) > 0) {
                return "<script>window.opener.location.reload(); window.close(); alert('탈퇴하시려면 판매 물품을 삭제하신 후에 다시 해주시기 바랍니다.');</script>";
            } else {
                sd.sellerDelete(id);
                session.invalidate();
                return "<script>window.opener.location.reload(); window.close(); alert('회원정보가 삭제되었습니다.');</script>";
            }
        }
        return "";
    }
    
    // 상품 관리 페이지
    @GetMapping("/seller/product")
    public String productList(HttpSession session,
                              Model model)
    {
        String id = (String)session.getAttribute("id");
        int ox = sd.sellerOX(id);

        // 셀러가 아니면 로그인 페이지로
        if (ox == 0) {
            return "redirect:/login";
        } 
        List<Map<String, Object>> productSelect = sd.productSelect(id);

        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        for (Map<String, Object> product : productSelect) {
            // prod_price 값을 가져와서 문자열로 변환합니다.
            String priceString = product.get("prod_price").toString();

            // 문자열 형태의 가격을 정수로 변환하여 포맷팅합니다.
            long price = Long.parseLong(priceString);
            product.put("prod_price_disp", decimalFormat.format(price));
        }
        model.addAttribute("productSelect", productSelect);

        return "/html/seller/productlist";
    }

    // 상품 등록 페이지
    @GetMapping("/seller/product/insert")
    public String productInsert(HttpSession session,
                                Model model)
    {
        String id = (String)session.getAttribute("id");
        int ox = sd.sellerOX(id);

        // 셀러가 아니면 로그인 페이지로
        if (ox == 0) {
            return "redirect:/login";
        } 
        List<Map<String, Object>> categoryList = sd.categoryList();
        model.addAttribute("categoryList", categoryList);


        return "/html/seller/productinsert";
    }

    // 상품 등록 액션
    @PostMapping("/seller/product/insert/action")
    public String productInsertAction(@RequestParam String category,
                                      @RequestParam String productTitle,
                                      @RequestParam String productName,
                                      @RequestParam MultipartFile file,
                                      @RequestParam String productDescription,
                                      @RequestParam String productAmount,
                                      @RequestParam String productPrice,
                                      @RequestParam String productGrade,
                                      @RequestParam String productVariety,
                                      HttpSession session,
                                      Model model) throws IOException
    {
        String id = (String)session.getAttribute("id");
        int ox = sd.sellerOX(id);

        // 셀러가 아니면 로그인 페이지로
        if (ox == 0) {
            return "redirect:/login";
        } 

        String imgName = null;
        // 파일이 업로드되었을 경우에만 처리
        if (file != null && !file.isEmpty()) {
            UUID uuid = UUID.randomUUID();
            String imgPath = "C:/kyhshop/src/main/resources/static/images/";
            String ogName = file.getOriginalFilename();
            imgName = uuid + "_" + ogName;
            String uploadImg = imgPath + imgName;

            File img = new File(uploadImg);
            file.transferTo(img);
        } else {
            imgName = "default_image.jpg";
        }

        sd.productInsert(category, productTitle, productDescription, productName, id, imgName, productAmount, productPrice, productGrade, productVariety);
        return "redirect:/seller/product";
    }

    // 연도별 차트 페이지
    @GetMapping("/chart/year")
    public String yearChart(@RequestParam(defaultValue = "상품") String grade,
                            @RequestParam(defaultValue = "후지") String variety,
                            Model model) {
        List<Map<String,Object>> salesData = sd.testViewByGradeVariety(grade,variety);
        List<Map<String,Object>> Gradeview = sd.Gradeview();
        List<Map<String,Object>> Varietyview = sd.Varietyview();

        model.addAttribute("salesData", salesData);
        model.addAttribute("Gradeview", Gradeview);
        model.addAttribute("Varietyview", Varietyview);
        model.addAttribute("selectedGrade", grade);
        model.addAttribute("selectedVariety", variety);
        return "html/seller/chart";
    }

    // 월별 차트 페이지
    @GetMapping("/chart/month")
    public String monthChart(@RequestParam(defaultValue = "2023") String year,
                             @RequestParam(defaultValue = "상품") String grade,
                             @RequestParam(defaultValue = "후지") String variety,
                             Model model) {
        List<Map<String,Object>> salesData = sd.monthChart(variety, grade, year);
        List<Map<String,Object>> Gradeview = sd.Gradeview();
        List<Map<String,Object>> Varietyview = sd.Varietyview();
        List<Map<String,Object>> yearView = sd.YearView();

        model.addAttribute("salesData", salesData);
        model.addAttribute("Gradeview", Gradeview);
        model.addAttribute("Varietyview", Varietyview);
        model.addAttribute("yearView", yearView);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedGrade", grade);
        model.addAttribute("selectedVariety", variety);
        return "html/seller/monthchart";
    }

    // 셀러 주문관리 페이지
    @GetMapping("seller/order/manage")
    public String sellerOrderManage(HttpSession session,
                                    Model model) {
        String id = (String)session.getAttribute("id");
        int ox = sd.sellerOX(id);
        // 셀러가 아니면 로그인 페이지로
        if (ox == 0) {
            return "redirect:/login";
        } 
        List<Map<String, Object>> saleProduct= sd.saleProduct(id);
        DecimalFormat decimalFormat = new DecimalFormat("#,###");

        for (Map<String, Object> product : saleProduct) {
            // prod_price 값을 가져와서 문자열로 변환합니다.
            String priceString = product.get("total_price").toString();

            // 문자열 형태의 가격을 정수로 변환하여 포맷팅합니다.
            long price = Long.parseLong(priceString);
            product.put("total_price_disp", decimalFormat.format(price));
        }
        model.addAttribute("saleProduct", saleProduct);
        return "html/seller/order_manage";
    }

    public class SaleProduct {
        private LocalDateTime dt;

        public String getFormattedDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분 ss초");
            return dt.format(formatter);
        }
    }

    // 배송 상태 변경 페이지
    @GetMapping("seller/delivery")
    public String sellerDelivery(@RequestParam String seq,
                                 HttpSession session,
                                 Model model)
    {
        String id = (String)session.getAttribute("id");
        int ox = sd.sellerOX(id);
        // 셀러가 아니면 로그인 페이지로
        if (ox == 0) {
            return "redirect:/login";
        }

        Map<String, Object> stateList = sd.selectDelivery(seq);

        model.addAttribute("stateList", stateList);

        return "html/seller/delivery";
    }
    
    // 송장 등록
    @PostMapping("seller/delivery/action")
    @ResponseBody
    public String sellerDeliveryAction(@RequestParam String seq,
                                       @RequestParam String deliveryNumber,
                                       HttpSession session,
                                       Model model)
    {
        String id = (String)session.getAttribute("id");
        int ox = sd.sellerOX(id);
        // 셀러가 아니면 로그인 페이지로
        if (ox == 0) {
            return "redirect:/login";
        }
        sd.changeDeliveryState(seq, deliveryNumber);

        return "<script>window.close(); alert('배송상태가 수정되었습니다.');</script>";
    }
}
