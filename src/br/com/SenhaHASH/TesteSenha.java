package br.com.SenhaHASH;

import org.mindrot.jbcrypt.BCrypt;

public class TesteSenha {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		System.out.println(BCrypt.checkpw("gabriel", "$2a$10$w5D2e6djW0s/wL3Tdukan.BkXm9lerEcyNWWHn0xjczUAQVKjnBoC"));

	}

}
