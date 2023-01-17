package br.com.grancoffee.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;

import com.sankhya.util.StringUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_atendente implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		if(linhas.length==1) {
			start(linhas,arg0);
		}	
	}
	
	public void start(Registro[] linhas,ContextoAcao arg0) throws Exception {
		String usuario = (String) arg0.getParam("ATENDENTE");
		BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
		String atendente = getTSIUSU(new BigDecimal(usuario)).asString("NOMEUSUCPLT");
		String email = (String) linhas[0].getCampo("EMAIL");
		//Timestamp data = (Timestamp) arg0.getParam("DTPREVISTA");
		//String formatTimestamp = StringUtils.formatTimestamp(data, "dd/MM/YYYY");
		Timestamp dataFinal = (Timestamp) linhas[0].getCampo("DTFECHAMENTO");
		Timestamp dtAtual = new Timestamp(System.currentTimeMillis());
		String descricaoAbreviada = StringUtils.substr(linhas[0].getCampo("DESCRICAO").toString(), 0, 100);
		
		BigDecimal idFlow = (BigDecimal) linhas[0].getCampo("IDFLOW");
		
		if(idFlow!=null) {
			arg0.mostraErro("<br/><br/>Chamado aberto pelo Flow \"Chamados / Projetos\" O atendente será determinado de acordo com as regras do FLOW!<br/><br/>");
		}
		
		/* comentário temporário
		if(data.before(reduzUmDia(dtAtual))) {
			arg0.mostraErro("Quer atender um chamado no passado rapá? da não!");
		}else {
			if(dataFinal!=null) {
				arg0.mostraErro("Chamado encerrado, não pode ser alterado o Atendente/Data!");
			}else {
				//enviarEmail(numos,email,atendente,formatTimestamp,descricaoAbreviada);
				setDados(linhas,arg0);
			}
			
		}
		*/
		setDados(linhas,arg0);
	}
	
	private Timestamp reduzUmDia(Timestamp data) {
		Calendar dataAtual = Calendar.getInstance();
		dataAtual.setTime(data);
		dataAtual.add(Calendar.DAY_OF_MONTH, -1);
		return new Timestamp(dataAtual.getTimeInMillis());
	}
	
	private void setDados(Registro[] linhas,ContextoAcao arg0) throws Exception {
		linhas[0].setCampo("ATENDENTE", arg0.getParam("ATENDENTE"));
		
		
		Timestamp data = (Timestamp) arg0.getParam("DTPREVISTA");
		if(data!=null) {
			linhas[0].setCampo("DTPREVISTA", arg0.getParam("DTPREVISTA"));
		}
		
	}
	
	private DynamicVO getTSIUSU(BigDecimal usuario) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { usuario });
		return VO;
	}
	
	private void enviarEmail(BigDecimal numos, String email, String atendente, String data, String descricao) throws Exception {
		
		try {
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "O seu chamado de número <b>"+numos+"</b>."
					+ "<br/><br/><i>\""+descricao+" ...\"</i>"
					+ "<br/><br/>será atendido por: "
					+ "<br/><br/><b>Atendente:</b> "+atendente
					+ "<br/><br/><b>Data prevista de atendimento:</b> "+data
					+ "<br/><br/><b>Esta é uma mensagem automática, por gentileza não respondê-la</b>"
					+ "<br/><br/>Atencionamente,"
					+ "<br/>Departamento TI"
					+ "<br/>Gran Coffee Comércio, Locação e Serviços S.A."
					+ "<br/>"
					+ "<img src=http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-pq.png  alt=\"\"/>";
			
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
			System.out.println("## [ChamadosTI.evento_criaOS] ## - NAO FOI POSSIVEL ENVIAR E-MAIL DE CONFIRMACAO DE ABERTURA DE CHAMADO"+e.getMessage());
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
