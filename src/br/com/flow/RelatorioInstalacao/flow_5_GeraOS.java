package br.com.flow.RelatorioInstalacao;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_5_GeraOS implements TarefaJava {
	
	private String observacao = "";
	private String idprocesso = "";
	public void executar(ContextoTarefa arg0) throws Exception {
		try {
			start(arg0);
		} catch (Exception e) {
			System.out.println("Nao foi possivel gerar a OS! "+e.getMessage());
		}
	}
	
	private void start(ContextoTarefa arg0) throws Exception {
		int nroItem = 0;
		int userSubOs = 0;
		Object idInstanceProcesso = arg0.getIdInstanceProcesso();
		Object contrato = arg0.getCampo("CD_CONTRATO");
		Object usuario = arg0.getCampo("SISTEMA_USUARIO");
		
		idprocesso = idInstanceProcesso.toString();
		
		if(usuario==null) {
			usuario = 0;
		}
				
		String problema = getMaquinas(idInstanceProcesso, contrato);
		
		BigDecimal numos = gerarCabecalhoOS(usuario,problema);
		
		if(numos!=null && numos.intValue()!=0) {
			
			arg0.setCampo("SISTEMA_NROS", numos.toString());

			nroItem = 1;
			userSubOs = 55;
			geraItemOS(numos, userSubOs, nroItem);
			
			arg0.setCampo("SISTEMA_OBSNOTA", observacao);

		}else {
			arg0.setCampo("SISTEMA_NROS", "Erro ao Gerar OS!");
		}
		
	}
	
	private String getMaquinas(Object idInstanceProcesso, Object contrato) throws Exception {
		
		String desc = "";
		String concat = "";
		String descFinal = "";
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_MAQUINASFLOW","this.IDINSTPRN = ? ", new Object[] { idInstanceProcesso }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

		String patrimonio = DynamicVO.asString("CODBEM");
		BigDecimal produto = getTCIBEM(patrimonio).asBigDecimal("CODPROD");
		String descricao = getTGFPRO(produto).asString("DESCRPROD");
		
		desc="INSTALAR - "+descricao+" - PATRIMÔNIO: "+patrimonio+"\n";
		concat = concat + desc;
		}
		
		descFinal = concat+"\n"+getPlanta(contrato);
		observacao = descFinal;
		
		return descFinal;

	}
	
	private DynamicVO getTCIBEM(String codbem) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });
		
		return VO;
	}
	
	private DynamicVO getTGFPRO(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		
		return VO;
	}
	
	private String getPlanta(Object contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PLANTAS");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND ID=?",new Object[] { contrato, new BigDecimal(1) });
		
		String endplan = VO.asString("ENDPLAN");
		
		return endplan;
	}
	
	private BigDecimal gerarCabecalhoOS(Object usuario, String problema){
		
		BigDecimal numos = BigDecimal.ZERO;
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(343598));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
		
			
			  NotaProdVO.setProperty("DHCHAMADA", dataAtual);
			  NotaProdVO.setProperty("DTPREVISTA",addDias(dataAtual,new BigDecimal(7)));
			  NotaProdVO.setProperty("NUMOS",null); 
			  NotaProdVO.setProperty("SITUACAO","P");
			  NotaProdVO.setProperty("CODUSUSOLICITANTE",new BigDecimal(0));
			  NotaProdVO.setProperty("CODUSURESP",new BigDecimal(0));
			  NotaProdVO.setProperty("DESCRICAO",problema);
			  NotaProdVO.setProperty("AD_MANPREVENTIVA", "N");
			  //NotaProdVO.setProperty("AD_CHAMADOTI", "S");
			  NotaProdVO.setProperty("CODATEND", new BigDecimal(0));
			  NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			  NotaProdVO.setProperty("AD_TELASAC", "S");
			  NotaProdVO.setProperty("CODCOS", new BigDecimal(1));
			  NotaProdVO.setProperty("AD_FLOW", idprocesso);

			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");
						
			return numos;

		} catch (Exception e) {
			System.out.println("Problema ao gerar cabecalho da OS!!"+e.getMessage());
			e.printStackTrace();
		}
		
		return numos;
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
	
	private void geraItemOS(BigDecimal numos, int codusu, int nroItem){
		
		Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ItemOrdemServico",new Object[]{new BigDecimal(343598),new BigDecimal(1)});
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			NotaProdVO.setProperty("NUMOS",numos);
			NotaProdVO.setProperty("NUMITEM", new BigDecimal(nroItem));
			NotaProdVO.setProperty("HRINICIAL", null); 
			NotaProdVO.setProperty("HRFINAL", null);
			NotaProdVO.setProperty("DHPREVISTA", addDias(dataAtual,new BigDecimal(7)));
			NotaProdVO.setProperty("INICEXEC", null); 
			NotaProdVO.setProperty("TERMEXEC", null); 
			NotaProdVO.setProperty("SERIE", null);
			NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
			NotaProdVO.setProperty("CODOCOROS", new BigDecimal(1));
			NotaProdVO.setProperty("SOLUCAO", " ");
			NotaProdVO.setProperty("CODUSU", new BigDecimal(codusu));
			NotaProdVO.setProperty("CORSLA", null);
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);


		} catch (Exception e) {
			System.out.println("Problema ao gerar Item da OS!!"+e.getMessage());
			e.printStackTrace();
		}
	}
	
}
