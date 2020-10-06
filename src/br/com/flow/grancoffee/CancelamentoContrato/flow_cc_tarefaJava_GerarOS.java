package br.com.flow.grancoffee.CancelamentoContrato;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import com.sankhya.util.TimeUtils;
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

public class flow_cc_tarefaJava_GerarOS implements TarefaJava {
	//int contador = 0;
	String tipoRetirada="";
	
	@Override
	public void executar(ContextoTarefa arg0) throws Exception {
		start(arg0);		
	}
	
	private void start(ContextoTarefa arg0) {
		Object idflow = arg0.getIdInstanceProcesso();
		verificaPlantas(idflow,arg0);
	}
	
	private void verificaPlantas(Object idflow,ContextoTarefa arg0) {
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT DISTINCT idplanta FROM AD_PATCANCELAMENTO WHERE IDINSTPRN="+idflow);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				BigDecimal planta = contagem.getBigDecimal("idplanta");
				if (planta!=null) {
					criarOsParaAhPlanta(idflow,planta,arg0);
				}
			}
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel determinar as plantas!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	private void criarOsParaAhPlanta(Object idflow,BigDecimal planta,ContextoTarefa arg0) throws Exception {
		Object usuarioInclusao = arg0.getCampo("SYS_USUARIOINCLUSAO");
		
		String quantidadeMaquinas="Quantidade Máquinas: "+contagemQuantidadeDeMaquinas(idflow,planta)+"\n";
		String Patrimonios = "Patrimônios: \n"+getPatrimonios(idflow,planta)+"\n";	
		String outrosCampos = getOutrosCampos(idflow,planta,usuarioInclusao);
		
		String descricao = this.tipoRetirada+quantidadeMaquinas+Patrimonios+outrosCampos;
		
		BigDecimal numos = gerarCabecalhoOS(idflow,descricao);
		if(numos.intValue()!=0) {
			int primeiro = 159;
			geraItemOS(numos,idflow,primeiro, 1);		
			salvaOsGerada(idflow,numos);
		}
		//arg0.setCampo("SYS_OS", descricao);
	}
	
	private String getPatrimonios(Object idflow,BigDecimal planta) {
		String patrimonios = "";
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PATCANCELAMENTO","this.IDINSTPRN = ? and this.IDPLANTA=? ", new Object[] { idflow,planta }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) 
			{
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			String codbem = (String) DynamicVO.getProperty("CODBEM");
			BigDecimal codprod = (BigDecimal) DynamicVO.getProperty("CODPROD");
			
			String escada = "";
			String rampa = "";
			String elevador = "";
			
			if("1".equals(DynamicVO.asString("ESCADA"))) {
				escada="Escada: SIM,";
			}else {escada="Escada: NAO,";}
			
			if("1".equals(DynamicVO.asString("RAMPA"))) {
				rampa="Rampa: SIM,";
			}else {rampa="Rampa: NAO,";}
			
			if("1".equals(DynamicVO.asString("ELEVADOR"))) {
				elevador="Elevador: SIM.";
			}else {elevador="Elevador: NAO.";}
			
			patrimonios+=codbem+" - "+getTgfpro(codprod).asString("DESCRPROD")+" - "+escada+rampa+elevador+"\n";
			}
			
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel obter os patrimonios!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
		
		return patrimonios;
	}
	
	private DynamicVO getTgfpro(BigDecimal produto) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("Produto");
		DynamicVO VO = DAO.findOne("CODPROD=?",new Object[] { produto });
		return VO;
	}
	
	private String getOutrosCampos(Object idflow, BigDecimal planta,Object usuarioInclusao) { 
		String descricao = "";
		try {
			
			JapeWrapper DAO = JapeFactory.dao("AD_FORMCANCELAMENTO");
			DynamicVO VO = DAO.findOne("IDINSTPRN=?",new Object[] { idflow });
			if(VO!=null) {
				//String tipoRetirada="";
				if("1".equals(VO.asString("TIPOCANCEL"))) {
					this.tipoRetirada="CANCELAMENTO PARCIAL \n";
				}else {
					this.tipoRetirada="CANCELAMENTO TOTAL \n";
				}
				String nomeplanta = "Planta: "+ getAdPlanta(VO.asBigDecimal("NUMCONTRATO"),planta).asString("NOMEPLAN")+"\n";
				String multa ="";
				if("1".equals(VO.asString("COBRARMULTA"))) {
					multa = "Multa: SIM - "+VO.asBigDecimal("MULTA")+"\n";
				}else {
					multa = "Multa: NÃO - "+VO.asString("JUSTIFICATIVAMULTA")+"\n";
				}
				String ultimaCobranca ="Última Cobrança: "+ VO.asString("ULTIMACOBRANCA")+"\n";
				String dtRetirada ="Data Retirada: "+ TimeUtils.formataDDMMYY(VO.asTimestamp("DTRETIRADA"))+"\n";
				String endereco ="Endereço: "+ getAdPlanta(VO.asBigDecimal("NUMCONTRATO"),planta).asString("ENDPLAN")+"\n";
				String integracao = "";
				if("1".equals(VO.asString("INTEGRACAO"))) {
					integracao = "Necessário Integração: SIM \n";
				}else {
					integracao = "Necessário Integração: NÃO \n";
				}
				String restricaoHorario="";
				if("1".equals(VO.asString("RESTRICAOHORARIO"))) {
					restricaoHorario = "Possui Restrições de Horário: SIM - "+VO.asString("DESCRRESTRICAO")+"\n";
				}else {
					restricaoHorario = "Possui Restrições de Horário: NÃO \n";
				}
				String acessorios = "";
				if("1".equals(VO.asString("RETIRAACESSORIOS"))) {
					acessorios = "Retirada de Acessórios: SIM - "+VO.asString("ACESSORIOS")+"\n";
				}else {
					acessorios = "Retirada de Acessórios: NÃO \n";
				}
				String contato= "Contato: "+VO.asString("CT_NOME")+", "+VO.asString("CT_TEL")+"\n";
				String email = "E-mail: "+VO.asString("CT_EMAIL")+"\n";
				String gestor = "Gestor do Contrato: "+usuarioInclusao(usuarioInclusao)+"\n";
				
				descricao=nomeplanta+multa+ultimaCobranca+dtRetirada+endereco+integracao+restricaoHorario+acessorios+contato+email+gestor;
			}
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel obter as demais descricoes!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
		
		return descricao;
	}
	
	private int contagemQuantidadeDeMaquinas(Object idflow, BigDecimal planta) {
		int qtd = 0;
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) AS QTD FROM AD_PATCANCELAMENTO WHERE IDINSTPRN="+idflow+" AND IDPLANTA="+planta);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				qtd = contagem.getInt("QTD");
			}
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel determinar as plantas!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
		
		return qtd;
	}
	
	private DynamicVO getAdPlanta(BigDecimal contrato, BigDecimal planta) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("PLANTAS");
		DynamicVO VO = DAO.findOne("NUMCONTRATO=? and ID=?",new Object[] { contrato,planta });	
		return VO;
	}
		
	private String usuarioInclusao(Object usuarioInclusao) throws Exception {
		String nomeUsuario = "";
		if(usuarioInclusao!=null) {
			JapeWrapper DAO = JapeFactory.dao("Usuario");
			DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { usuarioInclusao });
			nomeUsuario = VO.asString("NOMEUSUCPLT");
		}
		return nomeUsuario;
	}
	
	private DynamicVO getFormCancelmaento(Object idflow) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_FORMCANCELAMENTO");
		DynamicVO VO = DAO.findOne("IDINSTPRN=?",new Object[] { idflow });
		return VO;
	}
	
	private BigDecimal gerarCabecalhoOS(Object idflow, String descricao) throws Exception{
		
		BigDecimal numos = BigDecimal.ZERO;
		DynamicVO form = getFormCancelmaento(idflow);
		DynamicVO patrimonio = getUmPatrimonioDeExemplo(idflow);
		
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("OrdemServico",new BigDecimal(552465));
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();

			NotaProdVO.setProperty("DHCHAMADA", TimeUtils.getNow());
			NotaProdVO.setProperty("DTPREVISTA", TimeUtils.dataAddDay(TimeUtils.getNow(), 7));
			NotaProdVO.setProperty("DTFECHAMENTO", null);
			NotaProdVO.setProperty("MODELOVISIVELAPPOS",null);
			NotaProdVO.setProperty("NOMEMODELO",null);
			NotaProdVO.setProperty("NUMOS",null);
			NotaProdVO.setProperty("SITUACAO","P");
			NotaProdVO.setProperty("CODUSUSOLICITANTE", new BigDecimal(2379));		
			NotaProdVO.setProperty("CODUSURESP", new BigDecimal(2379));	
			NotaProdVO.setProperty("DESCRICAO", descricao);
			NotaProdVO.setProperty("SERIE",patrimonio.asString("CODBEM"));
			NotaProdVO.setProperty("CODBEM",patrimonio.asString("CODBEM"));
			NotaProdVO.setProperty("NUMCONTRATO",form.asBigDecimal("NUMCONTRATO"));
			NotaProdVO.setProperty("CODPARC",form.asBigDecimal("CODPARC"));
			NotaProdVO.setProperty("CODCONTATO",new BigDecimal(1));
			NotaProdVO.setProperty("AD_MANPREVENTIVA", "N");
			NotaProdVO.setProperty("CODATEND", new BigDecimal(2379));
			NotaProdVO.setProperty("AD_DTPREVISTAPREV", TimeUtils.getNow());
			NotaProdVO.setProperty("CODUSUFECH", null);
			NotaProdVO.setProperty("DHFECHAMENTOSLA", null);
			NotaProdVO.setProperty("TEMPOSLA", new BigDecimal(7000));
			NotaProdVO.setProperty("TEMPOGASTOSLA", null);
			NotaProdVO.setProperty("AD_CODIGOLIBERACAO", null);
			NotaProdVO.setProperty("AD_TELASAC", "S");
			NotaProdVO.setProperty("CODCOS", new BigDecimal(1));
			NotaProdVO.setProperty("CODCENCUS", getTCSCON(form.asBigDecimal("NUMCONTRATO")).asBigDecimal("CODCENCUS"));
			
			dwfFacade.createEntity(DynamicEntityNames.ORDEM_SERVICO,(EntityVO) NotaProdVO);
			numos = NotaProdVO.asBigDecimal("NUMOS");

			return numos;

		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel gerar o cabeçalho da OS!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
		return numos;
	}
	
	private void geraItemOS(BigDecimal numos,Object idflow, int usuario, int numitem) throws Exception{
		
		DynamicVO patrimonio = getUmPatrimonioDeExemplo(idflow);
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ItemOrdemServico",new Object[]{new BigDecimal(552465),new BigDecimal(numitem)});
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			NotaProdVO.setProperty("NUMOS",numos);			
			NotaProdVO.setProperty("HRINICIAL", null);
			NotaProdVO.setProperty("HRFINAL", null);
			NotaProdVO.setProperty("DHPREVISTA", TimeUtils.dataAddDay(TimeUtils.getNow(), 7));
			NotaProdVO.setProperty("DHLIMITESLA", TimeUtils.dataAddDay(TimeUtils.getNow(), 7));
			NotaProdVO.setProperty("INICEXEC", null);
			NotaProdVO.setProperty("TERMEXEC", null);
			NotaProdVO.setProperty("SERIE", patrimonio.asString("CODBEM"));
			NotaProdVO.setProperty("CODPROD", getTCIBEM(patrimonio.asString("CODBEM")).asBigDecimal("CODPROD"));
			NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
			NotaProdVO.setProperty("CODOCOROS", new BigDecimal(2));
			NotaProdVO.setProperty("SOLUCAO", " ");
			NotaProdVO.setProperty("CODUSU", new BigDecimal(usuario));
			NotaProdVO.setProperty("CORSLA", null);
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);

		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel gerar os itens da OS!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	private DynamicVO getUmPatrimonioDeExemplo(Object idflow) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_PATCANCELAMENTO");
		DynamicVO VO = DAO.findOne("IDINSTPRN=? and ROWNUM=1", new Object[] { idflow });
		return VO;
	}
		
	private DynamicVO getTCIBEM(String codbem) throws Exception{
		JapeWrapper DAO = JapeFactory.dao("Imobilizado");
		DynamicVO VO = DAO.findOne("CODBEM=?",new Object[] { codbem });
		return VO;	
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
	
	private void salvaOsGerada(Object idflow, BigDecimal numos) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_OSCANCELAMENTO");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODREGISTRO", new BigDecimal(1));
			VO.setProperty("IDINSTPRN", idflow);
			VO.setProperty("IDINSTTAR", new BigDecimal(0));
			VO.setProperty("IDTAREFA", "UserTask_1nvh3nu");
			VO.setProperty("NUMOS", numos);
			
			dwfFacade.createEntity("AD_OSCANCELAMENTO", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("## [flow_cc_tarefaJava_GerarOS] ## - Não foio possivel Salvar a OS gerada!");
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
	}
}
