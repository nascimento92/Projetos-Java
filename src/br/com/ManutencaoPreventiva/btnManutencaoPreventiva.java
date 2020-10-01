package br.com.ManutencaoPreventiva;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class btnManutencaoPreventiva implements AcaoRotinaJava {
	
	private int cont=0;
	private String validador;
	private int servicoDaOs = 100000;
	private int usuarioDaSubOs = 2195;
	
	//1.0
	public void doAction(ContextoAcao contexto) throws Exception {
		
		Registro[] linhas = contexto.getLinhas(); //[A]
		Timestamp primeiraOS, proximaOS, ultimaOS;//[A]
		String patrimonio;
		
		for(int i=0; i < linhas.length; i++){//[B]

			primeiraOS = (Timestamp) linhas[i].getCampo("DTPRIMEIRA"); //[C]
			proximaOS = (Timestamp) linhas[i].getCampo("DTPROXMANUTENCAO");//[C]
			ultimaOS = (Timestamp) linhas[i].getCampo("DTULTMANUTENCAO");//[C]
			String codbem = (String) linhas[i].getCampo("CODBEM");//[C]			

			if(validaContratoAtivo(codbem)){ //[D]

				if(primeiraOS==null && proximaOS==null){//[E]
					
					Timestamp dataAtual = new Timestamp(System.currentTimeMillis()); //[F]
					linhas[i].setCampo("DTPRIMEIRA", dataAtual); //[F]
					linhas[i].setCampo("DTULTMANUTENCAO", dataAtual); //[F]
					BigDecimal prazo = (BigDecimal) linhas[i].getCampo("PRAZO"); //[F]
					linhas[i].setCampo("DTPROXMANUTENCAO", addDias(dataAtual,prazo)); //[F]
					Timestamp dtProximaManutencao = (Timestamp) linhas[i].getCampo("DTPROXMANUTENCAO");//[F]
					
					BigDecimal numcontrato = null;//[F]
					numcontrato = getNumContrato(codbem);//[F]
					
					BigDecimal codparc = (BigDecimal) linhas[i].getCampo("CODPARC");//[F]
					Timestamp dtAbertura = (Timestamp) linhas[i].getCampo("DTPRIMEIRA");//[F]

					BigDecimal numos = gerarCabecalhoOS(codbem,dataAtual,dtProximaManutencao,numcontrato,codparc,prazo);//[F]
					
					DynamicVO tcibem = getTCIBEM(codbem);//[F]
					BigDecimal codprod = tcibem.asBigDecimal("CODPROD");//[F]
					cadastraServicoParaOhExecutante(codprod);//[F]
					
					geraItemOS(numos, codbem, prazo); //[F]
					
					linhas[i].setCampo("ULTIMAOS", numos);//[F]
					
					salvaMANPREVOS(numos,dtAbertura,codbem,numcontrato,codparc);//[F]
					
					cont++;//[F]
					
				}else if(proximaOS!=null && ultimaOS!=null){//[G]
					
					validador = null;
					patrimonio = (String) linhas[i].getCampo("CODBEM"); //[H]
					DynamicVO tcibem = getTCIBEM(patrimonio);//[H]
					BigDecimal codprod = tcibem.asBigDecimal("CODPROD");//[H]
					cadastraServicoParaOhExecutante(codprod);//[H]
					gerarOsSelecionadas(patrimonio, validador);//[H]
					
				}else if(proximaOS!=null && ultimaOS==null) {//[I]
					
					validador = "5";//[J]
					patrimonio = (String) linhas[i].getCampo("CODBEM");//[J]
					DynamicVO tcibem = getTCIBEM(patrimonio);//[J]
					BigDecimal codprod = tcibem.asBigDecimal("CODPROD");//[J]
					cadastraServicoParaOhExecutante(codprod);//[J]
					gerarOsSelecionadas(patrimonio, validador);//[J]
				}
				
				contexto.setMensagemRetorno(cont+" OS geradas!"); //[K]	
			}
		}		
	}
	
	//1.1
	private boolean validaContratoAtivo(String codbem) throws Exception{
		boolean valida = false;
	
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		
		if(contrato!=null) {
			
			if(contrato.equals(new BigDecimal(0))){
				validador="3";
				salvaObservaoInterna(codbem, new BigDecimal(0),validador);
				valida=false;
			}else{
				
				JapeWrapper DAO2 = JapeFactory.dao("Contrato");
				DynamicVO VO2 = DAO2.findOne("NUMCONTRATO=?",new Object[] { contrato });
				String ativo = VO2.asString("ATIVO");
				    
				if("S".equals(ativo)){
				    	valida=true;
				}else {
					validador="2";
					salvaObservaoInterna(codbem,new BigDecimal(0),validador);
				}	
			}
		}
	    return valida;
	}
	
	//PRIMEIRA OS
	
	//1.2
	private BigDecimal gerarCabecalhoOS(String codbem, Timestamp dtUltimaManutencao, Timestamp dtProximaManutencao,
			BigDecimal numcontrato, BigDecimal codparc, BigDecimal prazo){
		
		BigDecimal numos = BigDecimal.ZERO;
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(381744));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
		
			NotaProdVO.setProperty("DHCHAMADA", dataAtual);
			NotaProdVO.setProperty("DTPREVISTA",addDias(dataAtual,new BigDecimal(7)));
			NotaProdVO.setProperty("DTFECHAMENTO", null);
			NotaProdVO.setProperty("MODELOVISIVELAPPOS",null);
			NotaProdVO.setProperty("NOMEMODELO",null);
			NotaProdVO.setProperty("NUMOS",null);
			NotaProdVO.setProperty("SITUACAO","P");
			NotaProdVO.setProperty("CODUSUSOLICITANTE",getUsuLogado());		
			NotaProdVO.setProperty("CODUSURESP",getUsuLogado());	
			NotaProdVO.setProperty("DESCRICAO",montaDescricao(codbem,dtUltimaManutencao,dtProximaManutencao));
			NotaProdVO.setProperty("SERIE",codbem);
			NotaProdVO.setProperty("CODBEM",codbem);
			NotaProdVO.setProperty("NUMCONTRATO",numcontrato);
			NotaProdVO.setProperty("CODPARC",codparc);
			NotaProdVO.setProperty("CODCONTATO",new BigDecimal(1));
			NotaProdVO.setProperty("AD_MANPREVENTIVA", "S");
			NotaProdVO.setProperty("CODATEND", getUsuLogado());
			NotaProdVO.setProperty("AD_DTPREVISTAPREV", dataAtual);
			NotaProdVO.setProperty("CODUSUFECH", null);
			NotaProdVO.setProperty("DHFECHAMENTOSLA", null);
			NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			NotaProdVO.setProperty("TEMPOGASTOSLA", null);
			NotaProdVO.setProperty("AD_CODIGOLIBERACAO", null);
			NotaProdVO.setProperty("AD_TELASAC", "S");
			NotaProdVO.setProperty("CODCENCUS", getTCSCON(numcontrato).asBigDecimal("CODCENCUS"));
			
			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");

			return numos;

		} catch (Exception e) {
			System.out.println("Problema ao gerar cabecalho da OS!!");
			e.printStackTrace();
		}
		return numos;
	}
	
	//1.2.1
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
	
	//1.3
	private void geraItemOS(BigDecimal numos, String codbem, BigDecimal prazo){
		
		Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ItemOrdemServico",new Object[]{new BigDecimal(381744),new BigDecimal(1)});
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			NotaProdVO.setProperty("NUMOS",numos);			
			NotaProdVO.setProperty("HRINICIAL", null);
			NotaProdVO.setProperty("HRFINAL", null);
			NotaProdVO.setProperty("DHPREVISTA", addDias(dataAtual,new BigDecimal(7)));
			NotaProdVO.setProperty("DHLIMITESLA", addDias(dataAtual,new BigDecimal(7)));
			NotaProdVO.setProperty("INICEXEC", null);
			NotaProdVO.setProperty("TERMEXEC", null);
			NotaProdVO.setProperty("SERIE", codbem);
			NotaProdVO.setProperty("CODPROD", getTCIBEM(codbem).asBigDecimal("CODPROD"));
			NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
			NotaProdVO.setProperty("CODOCOROS", new BigDecimal(14));
			NotaProdVO.setProperty("SOLUCAO", " ");
			NotaProdVO.setProperty("CODUSU", new BigDecimal(2195));
			NotaProdVO.setProperty("CORSLA", null);
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);


		} catch (Exception e) {
			System.out.println("Problema ao gerar Item da OS!!");
			e.printStackTrace();
		}
	}
	
	//1.4
	private void salvaMANPREVOS(BigDecimal numos, Timestamp dtAbertura, String codbem, BigDecimal contrato, BigDecimal parceiro) throws Exception{
		
		
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
	}

	
	//OS QUE JÁ TIVERAM A PRIMEIRA GERADA
	
	//1.5
	private void gerarOsSelecionadas(String codbem, String validador) throws Exception{
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_MANUPREV","this.CODBEM = ? ", new Object[] { codbem }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
		
		abrirOS(DynamicVO, validador);
			
		}
	}
	
	//1.5.1
	private void abrirOS(DynamicVO DynamicVO, String validador) throws Exception{
		String codbem = null;
		Timestamp dtUltimaManutencao = null;
		Timestamp dtProximaManutencao = null;
		BigDecimal numcontrato = null;
		BigDecimal codparc = null;
		BigDecimal prazo = null;
		Timestamp dtPrevista = null;
		Timestamp encerramentoUltimaOs = null;
		BigDecimal ultimaOS = null;
		
		if("5".equals(validador)) {
			
			codbem = DynamicVO.asString("CODBEM");
			dtUltimaManutencao = DynamicVO.asTimestamp("DTPROXMANUTENCAO");
			dtProximaManutencao = DynamicVO.asTimestamp("DTPROXMANUTENCAO");
			//numcontrato = DynamicVO.asBigDecimal("NUMCONTRATO");
			numcontrato = getNumContrato(DynamicVO.asString("CODBEM"));
			//codparc = DynamicVO.asBigDecimal("CODPARC"); 
			codparc = getTCSCON(numcontrato).asBigDecimal("CODPARC");
			prazo = DynamicVO.asBigDecimal("PRAZO");
			dtPrevista = dtProximaManutencao;
			encerramentoUltimaOs = DynamicVO.asTimestamp("DTPROXMANUTENCAO");
			ultimaOS = DynamicVO.asBigDecimal("ULTIMAOS");
			
		}else {
			
			codbem = DynamicVO.asString("CODBEM");
			dtUltimaManutencao = DynamicVO.asTimestamp("DTULTMANUTENCAO");
			dtProximaManutencao = DynamicVO.asTimestamp("DTPROXMANUTENCAO");
			numcontrato = DynamicVO.asBigDecimal("NUMCONTRATO");
			codparc = DynamicVO.asBigDecimal("CODPARC");
			prazo = DynamicVO.asBigDecimal("PRAZO");
			dtPrevista = dtProximaManutencao;
			encerramentoUltimaOs = DynamicVO.asTimestamp("DTFIMOS");
			ultimaOS = DynamicVO.asBigDecimal("ULTIMAOS");
			
		}
		
		
		if(encerramentoUltimaOs!=null){ //valida se a ultima OS foi encerrada.
			
			BigDecimal numos = gerarCabecalhoOS(codbem,dtUltimaManutencao,dtProximaManutencao,numcontrato,codparc,prazo,dtPrevista, validador);
			geraItemOS(numos,codbem,prazo);
			atualizarInformacoes(numos,codbem);
			salvaHistorico(numos,codbem, numcontrato, dtPrevista, codparc);
			
			if(numos!=null){
				cont++;
			}	
		}else{
			if(ultimaOS!=null){
				validador="1";
				salvaObservaoInterna(codbem,ultimaOS,validador);
			}
		}
	
	}
	
	//1.5.1.1
	private BigDecimal gerarCabecalhoOS(String codbem,Timestamp dtUltimaManutencao,Timestamp dtProximaManutencao,BigDecimal numcontrato,BigDecimal codparc, BigDecimal prazo, Timestamp dtPrevista,String validador) throws Exception{
		BigDecimal numos = BigDecimal.ZERO;
		
		try {			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(381744));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
		
			NotaProdVO.setProperty("DHCHAMADA", dataAtual);
			NotaProdVO.setProperty("DTPREVISTA",addDias(dataAtual, new BigDecimal(7)));
			NotaProdVO.setProperty("DTFECHAMENTO", null);
			NotaProdVO.setProperty("MODELOVISIVELAPPOS",null);
			NotaProdVO.setProperty("NOMEMODELO",null);
			NotaProdVO.setProperty("NUMOS",null);
			NotaProdVO.setProperty("SITUACAO","P");
			NotaProdVO.setProperty("CODUSUSOLICITANTE",getUsuLogado());
			NotaProdVO.setProperty("CODUSURESP",getUsuLogado());
			NotaProdVO.setProperty("SERIE",codbem);
			NotaProdVO.setProperty("CODBEM",codbem);
			NotaProdVO.setProperty("NUMCONTRATO",numcontrato);
			NotaProdVO.setProperty("CODPARC",codparc);
			NotaProdVO.setProperty("CODCONTATO",new BigDecimal(1));
			NotaProdVO.setProperty("AD_MANPREVENTIVA", "S");
			NotaProdVO.setProperty("CODATEND", getUsuLogado());
			NotaProdVO.setProperty("AD_DTPREVISTAPREV", dtPrevista);
			NotaProdVO.setProperty("CODUSUFECH", null);
			NotaProdVO.setProperty("DHFECHAMENTOSLA", null);
			NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			NotaProdVO.setProperty("TEMPOGASTOSLA", null);
			NotaProdVO.setProperty("AD_CODIGOLIBERACAO", null);
			NotaProdVO.setProperty("AD_TELASAC", "S");
			NotaProdVO.setProperty("CODCENCUS", getTCSCON(numcontrato).asBigDecimal("CODCENCUS"));
			
			if("5".equals(validador)) {
				NotaProdVO.setProperty("DESCRICAO","** Manutenção Preventiva **\nPatrimônio: "+codbem);
			}else {
				NotaProdVO.setProperty("DESCRICAO",montaDescricao(codbem,dtUltimaManutencao,dtProximaManutencao,prazo));	
			}

			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");		

		return numos;
			
		} catch (Exception e) {
			System.out.println("ERRO AO GERAR CABEÇALHO DA OS!");
			e.printStackTrace();
		}
		
		return numos;		
	}
	
	//1.5.1.1.1
	private String montaDescricao(String codbem, Timestamp dtUltimaManutencao, Timestamp dtProximaManutencao, BigDecimal prazo) throws Exception{
		
		SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
		Date data = new Date(dtUltimaManutencao.getTime());
		String ultimaManutencaoFormatada = formato.format(data);
		
		data = new Date(addDias(dtUltimaManutencao,prazo).getTime());
		String proximaManutencaoFormatada = formato.format(data);
		
		return  "** Manutenção Preventiva **"+
				"\nPatrimônio: "+codbem+
				"\nData última Manutenção: "+ultimaManutencaoFormatada+
				"\nData Próxima Manutenção: "+proximaManutencaoFormatada+"*"+
				"\n*A data da próxima manutenção pode sofrer alterações, verificar com o responsável";
	}
	
	//1.5.1.2
	private void atualizarInformacoes(BigDecimal numos, String codbem) throws Exception{
		
		Timestamp dataAtual=new Timestamp(System.currentTimeMillis());
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("AD_MANUPREV", codbem);
		EntityVO NVO = PersistentLocalEntity.getValueObject();
		DynamicVO VO = (DynamicVO) NVO;
						
		VO.setProperty("ULTIMAOS", numos);
		VO.setProperty("DTULTMANUTENCAO", dataAtual);
		
		Timestamp datainicial = VO.asTimestamp("DTULTMANUTENCAO");
		BigDecimal prazo = VO.asBigDecimal("PRAZO");
		
		VO.setProperty("DTPROXMANUTENCAO", addDias(datainicial,prazo));
		VO.setProperty("DTFIMOS", null);
						
		PersistentLocalEntity.setValueObject(NVO);
	}
	
	//1.5.1.3
	private void salvaHistorico(BigDecimal numos,String codbem, BigDecimal contrato, Timestamp dataPrevista, BigDecimal parceiro) throws Exception{
		
		Timestamp dtAbertura =new Timestamp(System.currentTimeMillis());
		BigDecimal codusu = new BigDecimal(0);
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		EntityVO padraoNPVO = dwfFacade.getDefaultValueObjectInstance("AD_MANUPREVOS");
		DynamicVO prodservicoVO = (DynamicVO) padraoNPVO;
		
		prodservicoVO.setProperty("NUMOS", numos);
		prodservicoVO.setProperty("NUMCONTRATO", contrato);
		prodservicoVO.setProperty("DTPREVISTA", dataPrevista);		
		prodservicoVO.setProperty("DTABERTURA", dtAbertura);	
		prodservicoVO.setProperty("CODUSU", codusu);
		prodservicoVO.setProperty("CODPARC", parceiro);
		prodservicoVO.setProperty("CODBEM", codbem);
		
		//verificando se está atrasada
		Calendar dataAtual = new GregorianCalendar();
        Calendar dataPrevistaOS = new GregorianCalendar();
		
        dataAtual.setTime(dtAbertura);
        dataPrevistaOS.setTime(dataPrevista);
		
		if(dataAtual.getTimeInMillis()>dataPrevistaOS.getTimeInMillis()){
			prodservicoVO.setProperty("ATRASADA", "S");	
		}
	
		dwfFacade.createEntity("AD_MANUPREVOS", (EntityVO) prodservicoVO);
	}
	
	//1.5.1.4
	private void salvaObservaoInterna(String codbem, BigDecimal ultimaOS, String validador) throws Exception{
		
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		PersistentLocalEntity PersistentLocalEntity = dwfFacade.findEntityByPrimaryKey("AD_MANUPREV", codbem);
		EntityVO NVO = PersistentLocalEntity.getValueObject();
		DynamicVO VO = (DynamicVO) NVO;
		
		VO.setProperty("OBSERVACAOINTERNA"," ");
		
		if("1".equals(validador)){
			VO.setProperty("OBSERVACAOINTERNA", "Não foi possivel gerar a próxima OS! A OS "+ultimaOS+" ainda não foi encerrada!");	
		}
		if("2".equals(validador)){
			VO.setProperty("OBSERVACAOINTERNA", "Não foi possivel gerar a OS! Contrato INATIVO!");
		}
		if("3".equals(validador)){
			VO.setProperty("OBSERVACAOINTERNA", "Não foi possivel gerar a OS! Patrimonio está no contrato 0 (Zero)!");
		}

		PersistentLocalEntity.setValueObject(NVO);
	}
	
	//MÉTODOS UTEIS
	
	private Timestamp addDias(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.DAY_OF_MONTH, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}
	
	private DynamicVO getTCIBEM(String codbem) throws Exception{
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });
		return VO;	
	}
	
	private BigDecimal getNumContrato(String codbem) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });
		
		BigDecimal contrato = VO.asBigDecimal("NUMCONTRATO");
		
		return contrato;
	}
	
	private DynamicVO getTCSCON(BigDecimal contrato) throws Exception {
		DynamicVO VO = null;

		try {
			JapeWrapper DAO = JapeFactory.dao("Contrato");
			VO = DAO.findOne("NUMCONTRATO=?", new Object[] { contrato });
		} catch (Exception e) {
			e.getMessage();e.printStackTrace();
		}
		return VO;
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
			System.out.println("## MANUT. PREVENTIVA ## - NAO FOI POSSIVEL CADASTRAR NA ABA SERVICO POR EXECUTANTE! "+e.getMessage());
			e.getStackTrace();
		}
	}
	
	
	
}

