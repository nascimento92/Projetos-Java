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
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.DynamicEntityNames;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class flow_cc_tarefaJava_GerarOS implements TarefaJava {
	
	/**
	 * @author Gabriel
	 * 
	 * 19/11/2020 11:15 vs1.6 Gabriel Nascimento: implementado método [cadastraServicoParaOhExecutante] para cadastrar o serviço/produto para o executante.
	 * 16/12/2020 16:35 vs1.7 Gabriel Nascimento: implementado método [salvarException] para registrar as Exceptions, criado método [registraOsCriada] para salvar na AD_PATCANCELAMENTO qual o número da OS gerada.
	 * 13/05/2022 16:07 vs1.8 Nicolas Oliveira: Inserido funcionalidade para alteração do motivo da OS = Se Motivo Principal for Inadimplência (5) então o motivo da OS é RETIRADA - INADIMP (70), senão segue o fluxo padrão.
	 */
	
	//int contador = 0;
	String tipoRetirada="";
	private int servicoDaOs = 100000;
	
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
			salvarException("[verificaPlantas] Nao foi possivel verificar as plantas! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void criarOsParaAhPlanta(Object idflow,BigDecimal planta,ContextoTarefa arg0) throws Exception {
		Object usuarioInclusao = arg0.getCampo("SYS_USUARIOINCLUSAO");
		
		String quantidadeMaquinas="Quantidade Máquinas: "+contagemQuantidadeDeMaquinas(idflow,planta)+"\n";
		String Patrimonios = "Patrimônios: \n"+getPatrimonios(idflow,planta)+"\n";	
		String outrosCampos = getOutrosCampos(idflow,planta,usuarioInclusao);
		
		String descricao = this.tipoRetirada+quantidadeMaquinas+Patrimonios+outrosCampos;
		
		String motivoPrincipal = getFormCancelmaento(idflow).asString("MOTIVOCANCELPRINC");
		
		BigDecimal numos = gerarCabecalhoOS(idflow,descricao);
		if(numos.intValue()!=0) {
			int primeiro = 159;
			geraItemOS(numos,idflow,primeiro, 1, motivoPrincipal);		
			salvaOsGerada(idflow,numos);
			registraOsCriada(idflow,planta,numos);
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
			
			String contatoRetirada = "";
			
			if(DynamicVO.asString("CONTATO")!=null) {
				contatoRetirada = "Contato Retirada: "+DynamicVO.asString("CONTATO");
				patrimonios+=codbem+" - "+getTgfpro(codprod).asString("DESCRPROD")+" - "+escada+rampa+elevador+", "+contatoRetirada+"\n";
			}else {
				patrimonios+=codbem+" - "+getTgfpro(codprod).asString("DESCRPROD")+" - "+escada+rampa+elevador+"\n";
			}
				
			}
				
		} catch (Exception e) {
			salvarException("[getPatrimonios] Nao foi possivel obter os patrimonios! "+e.getMessage()+"\n"+e.getCause());
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
					BigDecimal multaFormatada = null;
					BigDecimal multaDigitada = VO.asBigDecimal("MULTA");
						if(VO.asBigDecimal("TAXA")!=null) {
							multaFormatada = multaDigitada.add(VO.asBigDecimal("TAXA"));
						}else {
							multaFormatada = multaDigitada;
						}
					multaFormatada=multaFormatada.setScale(2, BigDecimal.ROUND_HALF_EVEN);
					multa = "Multa: SIM - R$"+multaFormatada+"\n";
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
			salvarException("[getOutrosCampos] Nao foi possivel obter os campos da descricao da OS! "+e.getMessage()+"\n"+e.getCause());
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
			salvarException("[contagemQuantidadeDeMaquinas] Nao foi possivel contar a qtd de máquinas! "+e.getMessage()+"\n"+e.getCause());
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
			salvarException("[gerarCabecalhoOS] Nao foi possivel gerar a tcsose! "+e.getMessage()+"\n"+e.getCause());
		}
		return numos;
	}
	
	private void geraItemOS(BigDecimal numos,Object idflow, int usuario, int numitem, String motivoprincipal) throws Exception{
		
		BigDecimal motivoOs = null;
		if ("5".equals(motivoprincipal)) {
			motivoOs = new BigDecimal(70);
		} else {
			motivoOs = new BigDecimal(2);
		}
		
		DynamicVO patrimonio = getUmPatrimonioDeExemplo(idflow);
		BigDecimal produto = getTCIBEM(patrimonio.asString("CODBEM")).asBigDecimal("CODPROD");
		
		cadastraServicoParaOhExecutante(produto,usuario);
		
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
			NotaProdVO.setProperty("CODPROD", produto);
			NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
			NotaProdVO.setProperty("CODOCOROS",motivoOs);
			NotaProdVO.setProperty("SOLUCAO", " ");
			NotaProdVO.setProperty("CODUSU", new BigDecimal(usuario));
			NotaProdVO.setProperty("CORSLA", null);
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);

		} catch (Exception e) {
			salvarException("[geraItemOS] Nao foi possivel gerar a tcsite! "+e.getMessage()+"\n"+e.getCause());
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
			salvarException("[getTCSCON] Nao foi possivel obter o contrato! "+e.getMessage()+"\n"+e.getCause());
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
			salvarException("[salvaOsGerada] Nao foi possivel registrar a OS Gerada! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void cadastraServicoParaOhExecutante(BigDecimal produto, int usuario) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ServicoProdutoExecutante");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODSERV", new BigDecimal(this.servicoDaOs));
			VO.setProperty("CODUSU", new BigDecimal(usuario));
			VO.setProperty("CODPROD", produto);
			
			dwfFacade.createEntity("ServicoProdutoExecutante", (EntityVO) VO);
		} catch (Exception e) {
			e.getCause();
			e.getMessage();
			e.printStackTrace();
		}
	}
	
	private void registraOsCriada(Object idflow,BigDecimal planta, BigDecimal numos) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("AD_PATCANCELAMENTO","this.IDINSTPRN = ? and this.IDPLANTA=? ", new Object[] { idflow,planta }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;

			VO.setProperty("NUMOS", numos);

			itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[registraOsCriada] Nao foi possivel registrar a OS Gerada na tabela dos patrimonios! "+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private void salvarException(String mensagem) {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "flow_cc_tarefaJava_GerarOS");
			VO.setProperty("PACOTE", "br.com.flow.grancoffee.CancelamentoContrato");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);
		} catch (Exception e) {
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
}
