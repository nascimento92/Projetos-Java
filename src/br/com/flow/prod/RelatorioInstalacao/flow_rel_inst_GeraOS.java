package br.com.flow.prod.RelatorioInstalacao;

import java.math.BigDecimal;
import java.sql.ResultSet;
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
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class flow_rel_inst_GeraOS implements TarefaJava {
	
	/**
	 * Objeto para gerar uma OS no sistema, atraves do fluxo do flow.
	 * 
	 * @author gabriel.nascimento
	 * @versao 1.0
	 */
	
	private String observacao = "";
	private String idprocesso = "";
	private BigDecimal codprod = null;
	private BigDecimal parceiro = null;
	private BigDecimal parceiroDemonstracao = null;
	private int usuarioDaSubOs = 55;
	private int servicoDaOs = 100000;
	
	public void executar(ContextoTarefa arg0) throws Exception {
		
		start(arg0);
		/*
		 * try {
		 * 
		 * } catch (Exception e) { System.out.
		 * println("## FLOW - GERAÇÃO DA OS ## - Nao foi possivel gerar a OS! "+e.
		 * getMessage()); e.getStackTrace(); }
		 */
	}
	
	//1.0
	private void start(ContextoTarefa arg0) throws Exception {
		int nroItem = 0;
		int userSubOs = 0;
		
		//Pega os campos do processo
		Object idInstanceProcesso = arg0.getIdInstanceProcesso();
		String contrato = (String) arg0.getCampo("CD_CONTRATO");
		Object usuario = arg0.getCampo("SISTEMA_USUARIO"); //vai pegar o usuário da tarefa anterior
		
		
		String parcDem = (String) arg0.getCampo("CD_PARCDEMONSTRACAO");
		if(parcDem==null) {
			parcDem="0";
		}
		
		
		idprocesso = idInstanceProcesso.toString();
		
		if(usuario==null) {
			usuario = 0;
		}
		
		if(contrato==null) {
			contrato="1000";
		}
		
		//converte os campos
		BigDecimal numcontrato = new BigDecimal(contrato);
		BigDecimal parceiro = getTCSCON(numcontrato);
		BigDecimal parceiroDemonstracao = null;
		
		this.parceiro = parceiro;
		if(parcDem!=null) {
			parceiroDemonstracao = new BigDecimal(parcDem);
			this.parceiroDemonstracao = parceiroDemonstracao;
		}
		
		String problema = getMaquinas(idInstanceProcesso, numcontrato);
		
		if(problema==null) {
			problema="OS GERADA PELO FLOW";
		}
		
		BigDecimal numos = gerarCabecalhoOS(usuario,problema,numcontrato,parceiro,parceiroDemonstracao);
		
		if(numos!=null && numos.intValue()!=0) {
			
			arg0.setCampo("SISTEMA_NROS", numos.toString());

			nroItem = 1;
			userSubOs = usuarioDaSubOs;
			geraItemOS(numos, userSubOs, nroItem);
			
			arg0.setCampo("SISTEMA_OBSNOTA", observacao);

		}else {
			arg0.setCampo("SISTEMA_NROS", "Erro ao Gerar OS!");
		}
	}
	
	//1.1
	private String getMaquinas(Object idInstanceProcesso, BigDecimal contrato) throws Exception {
		
		String desc = "";
		String concat = "";
		String descFinal = "";
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_MAQUINASFLOW","this.IDINSTPRN = ? ", new Object[] { idInstanceProcesso }));

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

		PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
		DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			if (DynamicVO != null) {//se existir um registro na tabela
				String patrimonio = DynamicVO.asString("CODBEM");
				
				if(patrimonio!=null) {//se o código do bem não for vazio
					BigDecimal produto = getTCIBEM(patrimonio).asBigDecimal("CODPROD");
					codprod = produto;
					
					if (!validaSeOhServicoEstaCadastrado(produto)) {
						cadastraServicoParaOhExecutante(produto);
					}
					
					String descricao = getTGFPRO(produto).asString("DESCRPROD");
					desc = "INSTALAR - " + descricao + " - PATRIMÔNIO: " + patrimonio + "\n";
					concat = concat + desc;
				}else { //se não existir máquinas do fluxo
					concat = "NAO FORAM ESPECIFICADOS MÁQUINAS PARA O FLUXO";
				}
			}else {//se não existir nenhum registro na tabela
				concat = "NAO FORAM ESPECIFICADOS MÁQUINAS PARA O FLUXO";
			}
		}
		
		String planta = null;
		BigDecimal primeiroIdDasPlantas = getPrimeiroIdDasPlantas(contrato);
		
		if(primeiroIdDasPlantas==null) {
			planta="NÃO FORAM ENCONTRADAS PLANTAS CADASTRADAS NO CONTRATO";
		}else {
			planta = getPlanta(contrato,primeiroIdDasPlantas);
			if(planta==null) {
				planta="SEM ENDEREÇO ESPECIFICADO";
			}
		}
		
		descFinal = concat+"\n"+planta;
		observacao = descFinal;
		
		return descFinal;
	}
	
	private boolean validaSeOhServicoEstaCadastrado(BigDecimal produto) throws Exception {
		boolean valida=false;
		JapeWrapper DAO = JapeFactory.dao("ServicoProdutoExecutante");
		DynamicVO VO = DAO.findOne("CODSERV=? AND CODUSU=? AND CODPROD=?",new Object[] { this.servicoDaOs, this.usuarioDaSubOs, produto });
		
		if(VO!=null) {
			valida=true;
		}
		
		return valida;

	}
	
	//1.1.1
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
			System.out.println("## FLOW - GERAÇÃO DA OS ## - NAO FOI POSSIVEL CADASTRAR NA ABA SERVICO POR EXECUTANTE! "+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//1.2
	private BigDecimal gerarCabecalhoOS(Object usuario, String problema, BigDecimal contrato, BigDecimal parceiro, BigDecimal parceiroDemonstracao){
		
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
			  NotaProdVO.setProperty("CODUSUSOLICITANTE",new BigDecimal(2379));
			  NotaProdVO.setProperty("CODUSURESP",new BigDecimal(2379));
			  NotaProdVO.setProperty("CODUSUFECH",null);
			  NotaProdVO.setProperty("DHFECHAMENTOSLA",null);
			  NotaProdVO.setProperty("TEMPOGASTOSLA",null);
			  NotaProdVO.setProperty("AD_CODIGOLIBERACAO",null);
			  NotaProdVO.setProperty("DESCRICAO",problema);
			  NotaProdVO.setProperty("AD_MANPREVENTIVA", "N");
			  NotaProdVO.setProperty("CODATEND", new BigDecimal(2379));
			  NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			  NotaProdVO.setProperty("AD_TELASAC", "S");
			  NotaProdVO.setProperty("CODCOS", new BigDecimal(1));
			  NotaProdVO.setProperty("AD_FLOW", idprocesso);
			  NotaProdVO.setProperty("AD_PARCNESPRESSO", null); 
			  
			  if(parceiroDemonstracao.intValue()!=0) {
				  NotaProdVO.setProperty("CODPARC", parceiroDemonstracao);  
				  NotaProdVO.setProperty("NUMCONTRATO", new BigDecimal(1000));
			  }else {
				  NotaProdVO.setProperty("CODPARC", parceiro);
				  NotaProdVO.setProperty("NUMCONTRATO", contrato);
			  }
			 

			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");
						
			return numos;

		} catch (Exception e) {
			System.out.println("## FLOW - GERAÇÃO DA OS ## - PROBLEMA AO GERAR O CABEÇALHO DA OS! "+e.getMessage());
			e.getStackTrace();
		}
		
		return numos;
	}
	
	//1.3
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
			
			if(codprod!=null) {
				NotaProdVO.setProperty("CODPROD", codprod);
			}

			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);


		} catch (Exception e) {
			System.out.println("## FLOW - GERAÇÃO DA OS ## - PROBLEMA AO GERAR A SUB-OS! "+e.getMessage());
			e.getStackTrace();
		}
	}
	
	//MÉTODOS AUXILIARES ---------------------------------------------------
	
	//MÉTODO AUXILIAR - VERIFICA O PATRIMONIO NA TCIBEM ---------
	private DynamicVO getTCIBEM(String codbem) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });		
		return VO;
	}
	
	//MÉTODO AUXILIAR - VERIFICAR A TGFPRO DO PRODUTO DA MÁQUINA ------
	private DynamicVO getTGFPRO(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });	
		return VO;
	}
	
	//MÉTODO AUXILIAR - VERIFICA A PLANTA COM ID 1
	private String getPlanta(BigDecimal contrato, BigDecimal id) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PLANTAS");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? AND ID=?",new Object[] { contrato, id });
		
		String endplan=null;
		
		if(contrato.intValue()==1000) {
			
			if(this.parceiroDemonstracao.intValue()!=0) {
				endplan=descobreEnderecoDoParceiro(parceiroDemonstracao);
			}else {
				endplan=descobreEnderecoDoParceiro(this.parceiro);
			}
			
		}else {
			endplan = VO.asString("ENDPLAN");	
		}
		
		return endplan;
	}
	
	//MÉTODO AUXILIAR - DESCOBRE ENDERECO DO PARCEIRO
	private String descobreEnderecoDoParceiro(BigDecimal parceiro) throws Exception {
		String count = null;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT (ENDERECO||','||NUMEND||' '||COMPLEMENTO||' - '||NOMEBAI||' - '||CEP) AS ENDERECO FROM END_ENTREGA_GC WHERE CODPARC="+parceiro+" AND ROWNUM=1");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getString("ENDERECO");
		}
		
		String endereco = count;
		
		return endereco;

	}
	
	//MÉTODO AUXILIAR - PEGA O TCSCON DO CONTRATO
	private BigDecimal getTCSCON(BigDecimal contrato) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Contrato");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=?",new Object[] { contrato});
		BigDecimal parceiro = VO.asBigDecimal("CODPARC");	
		return parceiro;
	}
	
	//MÉTODO AUXILIAR PEGA O PRIMEIRO ID DA PLANTA DO CONTRATO
	private BigDecimal getPrimeiroIdDasPlantas(Object contrato) throws Exception {
		int count = 0;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MIN(ID) AS ID FROM AD_PLANTAS WHERE NUMCONTRATO="+contrato);
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("ID");
		}
		
		BigDecimal primeiroCodigo = new BigDecimal(count);
		
		return primeiroCodigo;
	}

	//MÉTODO AUXILIAR - ADICIONA DIAS A UMA OS
	private Timestamp addDias(Timestamp datainicial,BigDecimal prazo){
		GregorianCalendar gcm = new GregorianCalendar();
		Date data = new Date(datainicial.getTime());
		gcm.setTime(data);
		gcm.add(Calendar.DAY_OF_MONTH, prazo.intValue());
		data = gcm.getTime();
		Timestamp dataInicialMaisPrazo = new Timestamp(data.getTime());
		
		return dataInicialMaisPrazo;
	}
	

}
