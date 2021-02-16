package GabrielUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class EnviarEmailSankhya {
	
	/**
	 * Utilizado no objeto br.com.grancoffee.ChamadosTI.btn_statusOS
	 * 
	 * @param numos
	 * @param descricao
	 * @param statusAtual
	 * @param textoComplementar
	 * @param email
	 */
	public void EnviarEmail(BigDecimal numos,String descricao,String statusAtual,String textoComplementar,String email) {
		try {
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "O seu chamado de número <b>"+numos+"</b>.."
					+ "<br/><br/><i>\""+descricao+" ...\"</i>"
					+ "<br/><br/>teve o seu status alterado."
					+ "<br/><br/><b>Status Atual:</b> "+statusAtual
					+ "<br/><br/>Isso significa que: <i> "+textoComplementar+"</i>"
					+ "<br/><br/><b>Esta é uma mensagem automática, por gentileza não respondê-la</b>"
					+ "<br/><br/>Atencionamente,"
					+ "<br/>Departamento TI"
					+ "<br/>Gran Coffee Comércio, Locação e Serviços S.A."
					+ "<br/>"
					+ "<img src=https://grancoffee.com.br/wp-content/themes/gran-coffe/assets/img/logo-gran-coffee-black.svg  alt=\"\"/>";
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODFILA", getUltimoCodigoFila());
			VO.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("MENSAGEM", mensagem.toCharArray());
			VO.setProperty("TIPOENVIO", "E");
			VO.setProperty("ASSUNTO", new String("CHAMADO - "+numos));
			VO.setProperty("EMAIL", email);
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));	
			VO.setProperty("CODSMTP", new BigDecimal(1));
			VO.setProperty("MAXTENTENVIO", new BigDecimal(3));
			VO.setProperty("TENTENVIO", new BigDecimal(0));
			VO.setProperty("REENVIAR", "N");
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [ChamadosTI.evento_criaOS] ## - NAO FOI POSSIVEL ENVIAR E-MAIL INFORMANDO O STATUS"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	private BigDecimal getUltimoCodigoFila() throws Exception {
		int count = 0;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODFILA)+1 AS CODFILA FROM TMDFMG");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("CODFILA");
		}
		
		BigDecimal ultimoCodigo = new BigDecimal(count);
		
		return ultimoCodigo;
	}
}
