package br.com.gsn.financeiro;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_enviarEmailRetificando implements AcaoRotinaJava{
	int quantidadeDeEmailsEnviados = 0;
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		
		for(int i=0; i<linhas.length; i++) {
			enviarEmail(linhas[i]);
		}
		
		arg0.setMensagemRetorno("Foram enviados <b>"+quantidadeDeEmailsEnviados+"</b> e-mails");
	}
	
	public void enviarEmail(Registro linhas) {
		
		String enviado = (String) linhas.getCampo("ENVIADO");
		
		if(!"S".equals(enviado)) {
			try {
				String mensagemCompleta = new String();
				mensagemCompleta = "Prezado cliente,<br><br>"
						+ "Por favor, solicitamos que desconsidere o e-mail anterior sobre Notificação ExtraJudicial. Pedimos desculpas pelo transtorno ocorrido devido a uma falha no sistema.<br><br><b>"
						+ "Essa é uma mensagem automática, por gentileza não respondê-la.</b><br><br>"
						+ "<img src=\"https://logovtor.com/wp-content/uploads/2020/04/gran-coffee-vector-logo.png\" height=\"100\" width=\"200\"><br>"
						+ "<b>Atendimento</b>"
						+ "<br>Capitais e Regiões Metropolitanas: 4000-1572<br>"
						+ " Demais Localidades: 0800 016 1940  \r";
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
				DynamicVO VO = (DynamicVO) NPVO;
				VO.setProperty("CODFILA", getUltimoCodigoFila());
				VO.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
				VO.setProperty("MENSAGEM", mensagemCompleta.toCharArray());
				VO.setProperty("TIPOENVIO", "E");
				VO.setProperty("ASSUNTO", new String("Desconsiderar email : Notificação ExtraJudicial"));
				VO.setProperty("EMAIL", linhas.getCampo("EMAIL"));
				VO.setProperty("CODUSU", new BigDecimal(0));
				VO.setProperty("STATUS", "Pendente");
				VO.setProperty("CODCON", new BigDecimal(0));
				VO.setProperty("CODSMTP", getContaSmtpPrincipal());
				VO.setProperty("MAXTENTENVIO", new BigDecimal(3));
				VO.setProperty("TENTENVIO", new BigDecimal(0));
				VO.setProperty("REENVIAR", "N");
				VO.setProperty("CODUSU", new BigDecimal(0));
				dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
				
				this.quantidadeDeEmailsEnviados++;
				linhas.setCampo("ENVIADO", "S");
				linhas.setCampo("DTENVIO", TimeUtils.getNow());
				
			} catch (Exception e) {
				throw new Error("Erro "+e.getCause());			
			}
		}
	
	}
	
	private BigDecimal getUltimoCodigoFila() throws Exception {
		int count = 0;
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODFILA)+1 AS CODFILA FROM TMDFMG");
		ResultSet contagem = nativeSql.executeQuery();
		while (contagem.next())
			count = contagem.getInt("CODFILA");
		BigDecimal ultimoCodigo = new BigDecimal(count);
		return ultimoCodigo;
	}
	
	private BigDecimal getContaSmtpPrincipal() throws Exception {
		int count = 1;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODSMTP) AS COD FROM TSISMTP WHERE PADRAO = 'S'");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("COD");
		}

		BigDecimal codigoConta = new BigDecimal(count);

		return codigoConta;
	}

}
