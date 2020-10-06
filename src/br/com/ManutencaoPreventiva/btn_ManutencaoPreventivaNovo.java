package br.com.ManutencaoPreventiva;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btn_ManutencaoPreventivaNovo implements AcaoRotinaJava {
	
	private int servicoDaOs = 100000;
	private int usuarioDaSubOs = 2195;;
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		start(arg0);
	}
	
	private void start(ContextoAcao contexto) throws Exception {
		Registro[] linhas = contexto.getLinhas();
		
		for(int i=0; i < linhas.length; i++){
			String patrimonio = (String) linhas[i].getCampo("CODBEM");
			
			validacoes(patrimonio, linhas[i]);
			verificaSePrecisaGerarAhPrimeiraOuAhProximaOs(linhas[i],patrimonio);
		}
	}
	
	private void verificaSePrecisaGerarAhPrimeiraOuAhProximaOs(Registro linhas,String patrimonio) throws Exception {
		
		Timestamp dtPrimeiraOs = (Timestamp) linhas.getCampo("DTPRIMEIRA");
		Timestamp dtUltimaOs = (Timestamp) linhas.getCampo("DTULTMANUTENCAO");
		Timestamp dtProximaOs = (Timestamp) linhas.getCampo("DTPROXMANUTENCAO");
				
		if(dtPrimeiraOs==null) {
			gerarPrimeiraOs(patrimonio,linhas);
		}
	}
	
	private void gerarPrimeiraOs(String patrimonio,Registro linhas) throws Exception {
		linhas.setCampo("DTPRIMEIRA", TimeUtils.getNow());
		linhas.setCampo("DTULTMANUTENCAO", TimeUtils.getNow());
		linhas.setCampo("DTPROXMANUTENCAO", TimeUtils.dataAddDay(TimeUtils.getNow(), Integer.parseInt(linhas.getCampo("PRAZO").toString())));
		
		DynamicVO tciBem = getTciBem(linhas.getCampo("CODBEM").toString());
		DynamicVO getTcsCon = getTcsCon(tciBem.asBigDecimal("NUMCONTRATO"));
		
		BigDecimal numos = BigDecimal.ZERO;
		
		cadastraServicoParaOhExecutante(tciBem.asBigDecimal("CODPROD"));
		
		try { //gera cabeçalho
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(381744));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
					
			NotaProdVO.setProperty("DHCHAMADA", TimeUtils.getNow());
			NotaProdVO.setProperty("DTPREVISTA",TimeUtils.dataAddDay(TimeUtils.getNow(),7));
			NotaProdVO.setProperty("DTFECHAMENTO", null);
			NotaProdVO.setProperty("MODELOVISIVELAPPOS",null);
			NotaProdVO.setProperty("NOMEMODELO",null);
			NotaProdVO.setProperty("NUMOS",null);
			NotaProdVO.setProperty("SITUACAO","P");
			NotaProdVO.setProperty("CODUSUSOLICITANTE",getUsuLogado());		
			NotaProdVO.setProperty("CODUSURESP",getUsuLogado());	
			NotaProdVO.setProperty("DESCRICAO", montaDescricao(tciBem.asString("CODBEM"),TimeUtils.getNow(),TimeUtils.dataAddDay(TimeUtils.getNow(), Integer.parseInt(linhas.getCampo("PRAZO").toString()))));
			NotaProdVO.setProperty("SERIE",linhas.getCampo("CODBEM"));
			NotaProdVO.setProperty("CODBEM",linhas.getCampo("CODBEM"));
			NotaProdVO.setProperty("NUMCONTRATO",tciBem.asBigDecimal("NUMCONTRATO"));
			NotaProdVO.setProperty("CODPARC",getTcsCon.asBigDecimal("CODPARC"));
			NotaProdVO.setProperty("CODCENCUS",getTcsCon.asBigDecimal("CODCENCUS"));
			NotaProdVO.setProperty("CODCONTATO",new BigDecimal(1));
			NotaProdVO.setProperty("AD_MANPREVENTIVA", "S");
			NotaProdVO.setProperty("CODATEND", getUsuLogado());
			NotaProdVO.setProperty("AD_DTPREVISTAPREV", TimeUtils.getNow());
			NotaProdVO.setProperty("CODUSUFECH", null);
			NotaProdVO.setProperty("DHFECHAMENTOSLA", null);
			NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			NotaProdVO.setProperty("TEMPOGASTOSLA", null);
			NotaProdVO.setProperty("AD_CODIGOLIBERACAO", null);
			NotaProdVO.setProperty("AD_TELASAC", "S");
			
			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");
			
		} catch (Exception e) {
			System.out.println("## [btn_ManutencaoPreventivaNovo] ## - Nao foi possivel criar o cabecalho da OS!");
			e.getCause();
			e.getMessage();
		}
		
		if (numos.intValue() != 0) { //gera Item
			linhas.setCampo("ULTIMAOS", numos);
			salvaMANPREVOS(numos,TimeUtils.getNow(),tciBem.asString("CODBEM"),getTcsCon.asBigDecimal("NUMCONTRATO"),getTcsCon.asBigDecimal("CODPARC"));
			
			try {
				
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ItemOrdemServico",new Object[]{new BigDecimal(381744),new BigDecimal(1)});
				DynamicVO NotaProdVO = ModeloNPVO.buildClone();
				
				NotaProdVO.setProperty("NUMOS",numos);			
				NotaProdVO.setProperty("HRINICIAL", null);
				NotaProdVO.setProperty("HRFINAL", null);
				NotaProdVO.setProperty("DHPREVISTA", TimeUtils.dataAddDay(TimeUtils.getNow(),7));
				NotaProdVO.setProperty("DHLIMITESLA", TimeUtils.dataAddDay(TimeUtils.getNow(),7));
				NotaProdVO.setProperty("INICEXEC", null);
				NotaProdVO.setProperty("TERMEXEC", null);
				NotaProdVO.setProperty("SERIE", tciBem.asString("CODBEM"));
				NotaProdVO.setProperty("CODPROD", tciBem.asBigDecimal("CODPROD"));
				NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
				NotaProdVO.setProperty("CODOCOROS", new BigDecimal(14));
				NotaProdVO.setProperty("SOLUCAO", " ");
				NotaProdVO.setProperty("CODUSU", new BigDecimal(2195));
				NotaProdVO.setProperty("CORSLA", null);
				
				dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);


			} catch (Exception e) {
				System.out.println("## [btn_ManutencaoPreventivaNovo] ## - Nao foi possivel criar a sub-os!");
				e.getCause();
				e.getMessage();
			}
		}
		 
	}
	
	private void gerarOsEhSubOs();
	
	private void validacoes(String patrimonio, Registro linhas) throws Exception {
		DynamicVO tciBem = getTciBem(patrimonio);
		BigDecimal contrato = tciBem.asBigDecimal("NUMCONTRATO");
		BigDecimal numos = (BigDecimal) linhas.getCampo("ULTIMAOS");
		
		if("N".equals(getTcsCon(contrato).asString("ATIVO"))) {
			throw new PersistenceException("<br/>Patrimonio: <b>"+patrimonio+"</b>, Contrato <b>"+contrato+"</b> inativo, não é possível gerar uma OS Preventiva!<br/><br/>");	
		}
		
		if(numos!=null) {
			if("P".equals(getTcsOse(numos).asString("SITUACAO"))) {
				throw new PersistenceException("<br/>Patrimonio: <b>"+patrimonio+"</b>, OS <b>"+numos+"</b> está pendente, não é possível gerar uma nova OS Preventiva!<br/><br/>");
			}
		}
	}
	
	private String montaDescricao(String codbem, Timestamp dtUltimaManutencao, Timestamp dtProximaManutencao){
		
		SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
		Date data = new Date(dtUltimaManutencao.getTime());
		data = new Date(dtProximaManutencao.getTime());
		String proximaManutencaoFormatada = formato.format(data);
		
		return  "** Manutenção Preventiva **"+
				"\nPatrimônio: "+codbem+
				
				"\n\nData Próxima Manutenção: "+proximaManutencaoFormatada+"*"+
				"\n*A data da próxima manutenção pode sofrer alterações, verificar com o responsável";
	}
	
	private void salvaMANPREVOS(BigDecimal numos, Timestamp dtAbertura, String codbem, BigDecimal contrato, BigDecimal parceiro) throws Exception{
		
		try {
			
			BigDecimal codusu = getUsuLogado();
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO padraoNPVO = dwfFacade.getDefaultValueObjectInstance("AD_MANUPREVOS");
			DynamicVO prodservicoVO = (DynamicVO) padraoNPVO;
			
			prodservicoVO.setProperty("NUMOS", numos);
			prodservicoVO.setProperty("DTABERTURA", dtAbertura);
			prodservicoVO.setProperty("CODUSU", codusu);
			prodservicoVO.setProperty("CODBEM", codbem);
			prodservicoVO.setProperty("NUMCONTRATO", contrato);
			prodservicoVO.setProperty("DTPREVISTA", dtAbertura);		

			prodservicoVO.setProperty("CODPARC", parceiro);

			
			dwfFacade.createEntity("AD_MANUPREVOS", (EntityVO) prodservicoVO);
			
		} catch (Exception e) {
			System.out.println("## [btn_ManutencaoPreventivaNovo] ## - Nao foi possivel salvar no Historico, patrimonio: "+codbem);
			e.getCause();
			e.getMessage();
		}
		
	}
	
	private void cadastraServicoParaOhExecutante(BigDecimal produto) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ServicoProdutoExecutante");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODSERV", new BigDecimal(this.servicoDaOs));
			VO.setProperty("CODUSU", new BigDecimal(this.usuarioDaSubOs));
			VO.setProperty("CODPROD", produto);
			
			dwfFacade.createEntity("ServicoProdutoExecutante", (EntityVO) VO);
		} catch (Exception e) {
			e.getStackTrace();
		}
	}
	
	private DynamicVO getTciBem(String patrimonio) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { patrimonio });
		return VO;
	}
	
	private DynamicVO getTcsCon(BigDecimal numcontrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?",new Object[] { numcontrato });
		return VO;
	}
	
	private DynamicVO getTcsOse(BigDecimal numos) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("OrdemServico");
		DynamicVO VO = DAO.findOne("NUMOS=?",new Object[] { numos });
		return VO;
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}

}
