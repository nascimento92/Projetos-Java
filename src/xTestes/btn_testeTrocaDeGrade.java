package xTestes;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;

import com.sankhya.util.JdbcUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_testeTrocaDeGrade implements AcaoRotinaJava{

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		//getTeclasFilial(new BigDecimal(144), new BigDecimal(9), arg0, "7526");
		getTeclasFilial2(new BigDecimal(144), new BigDecimal(9), arg0, "7526");
	}
	
	public void getTeclasFilial(BigDecimal codprodde, BigDecimal filial, ContextoAcao arg0, String codbem) throws Exception {
		try {

			JapeWrapper DAO = JapeFactory.dao("AD_TROCAGRADEEMMASSA");
			Collection<DynamicVO> listaDeTeclasLocalizadas = DAO.find("this.CODPROD=? AND this.CODEMP=? AND this.CODBEM=?", new Object[] { codprodde, filial, codbem });
			String a = "";
			int q = 0;
			for (DynamicVO tecla : listaDeTeclasLocalizadas) {
				 q++;
				 a+=tecla.asBigDecimal("TECLA")+",";		
			}
			
			arg0.setMensagemRetorno("Quantidade teclas: "+q+"\nTeclas: "+a);
			
		} catch (Exception e) {
			
		}
	}
	
	private void getTeclasFilial2(BigDecimal codprodde, BigDecimal filial, ContextoAcao arg0, String codbem) throws Exception {
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.addParameter(codprodde);
		nativeSql.addParameter(filial);
		nativeSql.addParameter(codbem);
		nativeSql.appendSql("SELECT TECLA FROM AD_TROCAGRADEEMMASSA WHERE CODPROD=? AND CODEMP=? AND CODBEM=?");
		contagem = nativeSql.executeQuery();
		
		String a = "";
		int q = 0;
		
		while (contagem.next()) {
			q++;
			a+=contagem.getBigDecimal("TECLA")+",";	
		}
		
		JdbcUtils.closeResultSet(contagem);
		NativeSql.releaseResources(nativeSql);
		
		arg0.setMensagemRetorno("Quantidade teclas: "+q+"\nTeclas: "+a);
	}

}
