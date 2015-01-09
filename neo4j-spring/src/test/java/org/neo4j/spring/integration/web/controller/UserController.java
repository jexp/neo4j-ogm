package org.neo4j.spring.integration.web.controller;

import org.neo4j.spring.integration.web.domain.User;
import org.neo4j.spring.integration.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/user/{name}/friends")
    @ResponseBody
    public String listFriends(@PathVariable String name, HttpSession session) {
        System.out.println("Session: " + session);
        User user = userService.getUserByName(name);

        if (user == null) {
            return "No such user!";
        }

        StringBuilder result = new StringBuilder();
        for (User friend : userService.getNetwork(user)) {
            result.append(friend.getName()).append(" ");
        }

        return result.toString().trim();
    }
}