package com.hmdp.interceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class UserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 获取session
        HttpSession session = request.getSession();

        // 获取session中的user对象
        Object user = session.getAttribute("user");

        // 如果user为空，返回401
        if (user == null) {
            response.setStatus(401);

            return false;
        }
        // 如果user不为空，将user保存到UserHolder中
        UserHolder.saveUser((UserDTO) user);

        // 放行
        return true;
    }

    // 移除UserHolder中的user
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
