package xTestes;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import com.sankhya.util.JdbcUtils;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class chamaProcedure {

	public static void main(String[] args) throws SQLException {
	
		CallableStatement cstm = null;
		try {
			EntityFacade entityFacade = EntityFacadeFactory.getDWFFacade();
			JdbcWrapper jdbc = entityFacade.getJdbcWrapper();
			
			Connection conn = jdbc.getConnection();
			cstm = conn.prepareCall("{call STP_CONTROLE_TCBSAL(?)}");
			//cstmt.setString(1, status == LIGA ? "S" : "N");
			cstm.execute();
		} finally {
			JdbcUtils.closeStatement(cstm);
		}
	}

}
