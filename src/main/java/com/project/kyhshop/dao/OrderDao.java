package com.project.kyhshop.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OrderDao {
    @Autowired
    JdbcTemplate jt;

    // 상품 아이디 구매전인 상태로 생성
    public void orderBasketInsertAction(String productId, String userId){
        String sqlStmt = "insert into order_mst (product_id, user_id,order_state) values(?,?,0)";
        jt.update(sqlStmt,productId,userId);
    }
   
    // 상품 자세히 보기
    public List<Map<String,Object>> orderBasketdetail(String productId, String userId){
        String sqlStmt = "select * from order_mst where order_state=0 and product_id =? and user_id=?";
        return jt.queryForList(sqlStmt,productId,userId);
    }

    
    // 상세보기 -> 장바구니
    public void orderBuyChange(String amount, String userId, String product_id) {
        String sqlStmt = "INSERT INTO order_mst(order_state,amount,user_id,product_id) VALUES(1, ?, ?, ?)";
        jt.update(sqlStmt, amount, userId , product_id);
    }  

    // 상품 상세보기 -> 구매
    public void orderBasketChange(String amount, String userId, String product_id) {
        String sqlStmt = "INSERT INTO order_mst(order_state,amount,user_id,product_id) VALUES(0, ?, ?, ?)";
        jt.update(sqlStmt, amount, userId , product_id);
    }  

    // 구매 목록보기 (구매자가 같은)
    public List<Map<String,Object>> orderBuyInsertAction2(String userId){
        String sqlStmt = "select * from order_mst where user_id=? and order_state=1";
        return jt.queryForList(sqlStmt,userId);
    }

 // ----------------------------------------------------------------------------------------------

    public List<Map<String,Object>> MypageOrder(String userId) {
        String sqlStmt = "select * from order_mst where order_state=0 and user_id='"+userId+"'";
        return jt.queryForList(sqlStmt);
    }  

    public List<Map<String,Object>> MypageBuy(String userId) {
        String sqlStmt = "select * from order_mst where order_state=1 and user_id='"+userId+"'";
        return jt.queryForList(sqlStmt);
    }  


    // 장바구니 -> 구매완료
    public void orderBasketChangeAction(String userId) {
        String sqlStmt = "UPDATE order_mst SET order_state = 1 WHERE user_id = ?";
        jt.update(sqlStmt, userId);
    }

    ////////////

    // 재고 수량 가져오기
    public int getAmount(String seq) {
        String sqlStmt = "SELECT prod_amount FROM tb_product_detail WHERE seq = ?";
        return jt.queryForObject(sqlStmt, Integer.class, seq);
    }

    // 메인 배송지 가져오기
    public Map<String, Object> getMainAddress(String id) {
        String sqlStmt = "SELECT name, phone, address, address_detail FROM tb_address_mst WHERE id = ? AND main_address = 1";
        try {
            return jt.queryForMap(sqlStmt, id);
        } catch (EmptyResultDataAccessException e) {
            return new HashMap<>(); // 결과가 없을 경우 빈 맵 반환
        }
    }

    // 주문 정보 가져오기
    public List<Map<String, Object>> orderSelect(String seq, int amount)
    {
        String sqlStmt = "SELECT    PM.prod_nm      AS prod_nm, " +
                         "          PD.prod_price   AS prod_price, " +
                         "          ?               AS amount, " +
                         "         (PD.prod_price * ?) AS total_price " +
                         "FROM      tb_product_mst PM " +
                         "          JOIN tb_product_detail PD ON PM.seq = PD.seq " +
                         "WHERE     PM.seq = ?";
        return jt.queryForList(sqlStmt, amount, amount, seq);
    }
}