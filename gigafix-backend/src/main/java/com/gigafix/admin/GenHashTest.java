package com.gigafix.admin;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenHashTest {
	public static void main(String[] args) {
		    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		    System.out.println(encoder.encode("456"));

    }
}
