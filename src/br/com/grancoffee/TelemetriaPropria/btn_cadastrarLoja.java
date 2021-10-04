package br.com.grancoffee.TelemetriaPropria;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import javax.swing.Timer;
import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

import Helpers.WSPentaho;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.ws.ServiceContext;

public class btn_cadastrarLoja implements AcaoRotinaJava {

	/**
	 * 24/09/21 vs 1.7 Ajustado o método cadastrarTelaInstalacoes estava inserindo um campo que não existe mais.
	 */
	String erro = "";

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}

	private void start(ContextoAcao arg0) throws InterruptedException {
		String nome = (String) arg0.getParam("NOME");
		String endereco = (String) arg0.getParam("ENDERECO");
		String contrato =  (String) arg0.getParam("CONTRATO");
		String telemetria = (String) arg0.getParam("TELEMETRIA");
		String tipo = (String) arg0.getParam("TIPO");

		String totem = totem(tipo);
		
		//arg0.setMensagemRetorno(totem);
		
		cadastrarAdPatrimonio(totem,nome,contrato);
		cadastrarTelaInstalacoes(totem,contrato,endereco);
		cadastrarTelemetria(totem,telemetria);
		
		Timer timer = new Timer(10000, new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				chamaPentahoCadastrarLoja();		
			}
		});
		timer.setRepeats(false);
		timer.start();
				 
		if (erro != "") {
			arg0.setMensagemRetorno("Erro \n" + erro);
		} else {
			arg0.setMensagemRetorno("Loja Cadastrada! - <b>" + totem + "</b>");
		}
	}

	private String totem(String tipo) {
		DecimalFormat df = new DecimalFormat("0000");
		String ultimoTotem = "";
		String novoTotem="";
		
		ultimoTotem = getUltimo(tipo);
		novoTotem = tipo + df.format(Integer.parseInt(StringUtils.substr(ultimoTotem, 4, 4)) + 1);
		
		return novoTotem;
		
	}

	private String getUltimo(String tipo) {
		
		String bem = tipo+"0000";
				
		try {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT MAX(CODBEM) AS BEM FROM AD_PATRIMONIO WHERE CODBEM LIKE '%"+tipo+"%' AND CODBEM NOT IN ('GCETESTE')");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				String result = contagem.getString("BEM");
				
				if(result!=null) {
					bem = result;
				}
				
			}
		} catch (Exception e) {
			erro = "Não foi possível determinar o último Corner!" + e.getMessage()+"\n"+e.getCause();
			salvarException(erro);
		}

		return bem;
	}

	private void cadastrarAdPatrimonio(String totem, String descricao,String contrato) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("PATRIMONIO");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("CODBEM", totem);
			VO.setProperty("DESCBEM", descricao.trim().toUpperCase().replaceAll("\\s+", " "));
			VO.setProperty("AD_INTENVIA", "S");
			VO.setProperty("ENVIADO_GCW", "N");
			VO.setProperty("CODPROD", new BigDecimal(514410));
			
			if(contrato!=null) {
				VO.setProperty("NUMCONTRATO", new BigDecimal(contrato));
				VO.setProperty("CODPARC", getParceiro(new BigDecimal(contrato)));
			}

			dwfFacade.createEntity("PATRIMONIO", (EntityVO) VO);
		} catch (Exception e) {
			erro = "Não foi possível cadastrar a loja!" + e.getMessage()+"\n"+e.getCause();
			salvarException(erro);
		}
	}
	
	private void cadastrarTelaInstalacoes(String totem,String contrato,String endereco) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCInstalacao");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("ABASTECIMENTO", "S");
			VO.setProperty("AD_IDPLANTA", new BigDecimal(endereco));
			VO.setProperty("CODBEM", totem);
			VO.setProperty("PLANOGRAMAPENDENTE", "N");
			VO.setProperty("AD_ENVIARPLANOGRAMA", "S");
			VO.setProperty("TOTEM", "S");
			
			if(contrato!=null) {
				VO.setProperty("AD_NUMCONTRATO", new BigDecimal(contrato));
			}
			
			dwfFacade.createEntity("GCInstalacao", (EntityVO) VO);
			
		} catch (Exception e) {
			erro = "Não foi possível cadastrar na tela Instalações!" + e.getMessage()+"\n"+e.getCause();
			salvarException(erro);
		}
	}
	
	private void cadastrarTelemetria(String totem, String telemetria) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("GCTelemInstalacao");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("ID", new BigDecimal(1));
			VO.setProperty("IDTEL", new BigDecimal(telemetria));
			VO.setProperty("CODBEM", totem);
			VO.setProperty("PRINCIPAL", "S");

			dwfFacade.createEntity("GCTelemInstalacao", (EntityVO) VO);

		} catch (Exception e) {
			erro = "Não foi possível cadastrar a telemtria!" + e.getMessage() + "\n" + e.getCause();
			salvarException(erro);
		}
	}
	
	private BigDecimal getParceiro(BigDecimal contrato) throws Exception {
		BigDecimal codparc = BigDecimal.ZERO;
		
		try {
			JapeWrapper DAO = JapeFactory.dao("Contrato");
			DynamicVO VO = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato });
			if(VO!=null) {
				codparc = VO.asBigDecimal("CODPARC");
			}
		} catch (Exception e) {
			erro = "Não foi possível determinar qual o contrato!" + e.getMessage()+"\n"+e.getCause();
			salvarException(erro);
		}
		
		return codparc;
	}
	
	private void chamaPentahoCadastrarLoja() {

		try {
			final String parameter = (String) MGECoreParameter.getParameter("PENTAHOIP");
			String site = parameter;
			//String site = "http://pentaho.grancoffee.com.br:8080/pentaho/kettle/";
			String Key = "Basic ZXN0YWNpby5jcnV6OkluZm9AMjAxNQ==";
			WSPentaho si = new WSPentaho(site, Key);

			String path = "home/GC_New/Transformation/Uppay-Cadastrar_Loja/";
			String objName = "J-Cadastrar_loja_uppay";

			si.runJob(path, objName);

		} catch (Exception e) {
			erro = "Não foi possível chamar a Rotina Pentaho!" + e.getMessage() + "\n" + e.getCause();
			salvarException(erro);
		}
	}
		
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "btn_cadastrarLoja");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);
			
			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
			
		} catch (Exception e) {
			//aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! "+e.getMessage());
		}
	}
}
