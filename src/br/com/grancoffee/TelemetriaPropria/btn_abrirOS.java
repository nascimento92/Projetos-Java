package br.com.grancoffee.TelemetriaPropria;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_abrirOS implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		String patrimonio = (String) arg0.getParam("CODBEM");
		
		BigDecimal numos = gerarCabecalhoOS(arg0, patrimonio);
		
		if(numos!=null && numos.intValue()!=0) {
			geraItemOS(numos, patrimonio, arg0);
			arg0.setMensagemRetorno("OS <b>"+numos+"</b> GERADA PARA O PATRIMONIO <b>"+patrimonio);
		}
		
	}
	
	private BigDecimal gerarCabecalhoOS(ContextoAcao arg0, String patrimonio) throws Exception{
		
		String problema = (String) arg0.getParam("DESCRICAO");
		
		BigDecimal numos = BigDecimal.ZERO;
		
		DynamicVO adPatrimonio = getAdPatrimonio(patrimonio);
		BigDecimal contrato = adPatrimonio.asBigDecimal("NUMCONTRATO");
		BigDecimal parceiro = BigDecimal.ZERO;
		
		if(contrato.intValue()!=0) {
			DynamicVO tcscon = getTcscon(contrato);
			parceiro = tcscon.asBigDecimal("CODPARC");
		}else {
			contrato = new BigDecimal(1314);
			parceiro = new BigDecimal(1);
		}
		
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(414937));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
			BigDecimal usuario = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();

			NotaProdVO.setProperty("DHCHAMADA", dataAtual);
			NotaProdVO.setProperty("DTPREVISTA",addDias(dataAtual,new BigDecimal(7)));
			NotaProdVO.setProperty("NUMOS",null); 
			NotaProdVO.setProperty("SITUACAO","P");
			NotaProdVO.setProperty("CODUSUSOLICITANTE",usuario);
			NotaProdVO.setProperty("CODUSURESP",usuario);
			NotaProdVO.setProperty("DESCRICAO",problema);
			NotaProdVO.setProperty("AD_MANPREVENTIVA", "N");
			NotaProdVO.setProperty("AD_CHAMADOTI", "N");
			NotaProdVO.setProperty("CODATEND", usuario);
			NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			NotaProdVO.setProperty("AD_TELASAC", "S");
			NotaProdVO.setProperty("CODCOS", new BigDecimal(1));
			NotaProdVO.setProperty("NUMCONTRATO", contrato);
			NotaProdVO.setProperty("CODPARC", parceiro);
			
			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");
			
		} catch (Exception e) {
			salvarException("[gerarCabecalhoOS] - NAO FOI POSSIVEL GERAR CABECALHO DA OS, patrimonio:"+patrimonio+"\n"+e.getMessage()+"\n"+e.getCause());
		}
		
		return numos;
	}
	
	private void geraItemOS(BigDecimal numos, String patrimonio, ContextoAcao arg0) throws Exception{
		
		Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
		
		String motivo = (String) arg0.getParam("MOTIVO");
		String executante = (String) arg0.getParam("EXECUTANTE");
		BigDecimal codprod = getCodProd(patrimonio);
			
		cadastraServicoParaOhExecutante(new BigDecimal(executante),codprod);
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ItemOrdemServico",new Object[]{new BigDecimal(414937),new BigDecimal(1)});
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			NotaProdVO.setProperty("NUMOS",numos);
			NotaProdVO.setProperty("NUMITEM",new BigDecimal(1));
			NotaProdVO.setProperty("HRINICIAL", null); 
			NotaProdVO.setProperty("HRFINAL", null);
			NotaProdVO.setProperty("DHPREVISTA", addDias(dataAtual,new BigDecimal(7)));
			NotaProdVO.setProperty("INICEXEC", null); 
			NotaProdVO.setProperty("TERMEXEC", null); 
			NotaProdVO.setProperty("SERIE", patrimonio);
			NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
			NotaProdVO.setProperty("CODOCOROS", new BigDecimal(motivo));
			NotaProdVO.setProperty("SOLUCAO", " ");
			NotaProdVO.setProperty("CODUSU", new BigDecimal(executante));
			NotaProdVO.setProperty("CODSERV", new BigDecimal(200000));
			NotaProdVO.setProperty("CODPROD", codprod);
			NotaProdVO.setProperty("CORSLA", new BigDecimal(11909048));
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);


		} catch (Exception e) {
			salvarException("[geraItemOS] - NAO FOI POSSIVEL GERAR A SUB-OS, patrimonio:"+patrimonio+"\n"+" os: "+numos+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private Timestamp addDias(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.DAY_OF_MONTH, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}
	
	private BigDecimal getCodProd(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		return produto;
	}
	
	private DynamicVO getAdPatrimonio(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PATRIMONIO");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		return VO;
	}
	
	private DynamicVO getTcscon(BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato });
		return VO;
	}
	
	private void cadastraServicoParaOhExecutante(BigDecimal usuario, BigDecimal produto) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ServicoProdutoExecutante");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODSERV", new BigDecimal(200000));
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("CODPROD", produto);
			
			dwfFacade.createEntity("ServicoProdutoExecutante", (EntityVO) VO);
		} catch (Exception e) {
			//salvarException("[cadastraServicoParaOhExecutante] - Nao foi possivel cadastrar o serviço para o executante. "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_abrirOS");
			VO.setProperty("PACOTE", "br.com.grancoffee.TelemetriaPropria");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}

}
