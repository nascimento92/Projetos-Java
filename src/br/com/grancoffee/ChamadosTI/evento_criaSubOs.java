package br.com.grancoffee.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceException;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
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

public class evento_criaSubOs implements EventoProgramavelJava {
	// ID da tratativa n pode ser somado automaticamente
	/**
	 * 28/10/20 14:11 - Inserido no método beforeupdate para considerar se o chamado não está sendo reaberto.
	 * 07/04/21 10:14 - Inserido método para registrar as Exceptions.
	 */
	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {

		if (getUsuLogado().intValue() != 0) {
			throw new PersistenceException(
					"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"
							+ "\n<font size=\"20\"><b>Atividades não podem ser excluidas!</b></font>\n<br/><br/>");
		}
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		if(validacoes(VO)) {
			throw new PersistenceException(
					"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
					"\n<font size=\"20\"><b>Antes de cadastrar uma atividade classifique o chamado e defina um atendente!</b></font>\n<br/><br/>");
		}
		
		if(!validaUsuario(getUsuLogado())) {
			throw new PersistenceException(
					"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
					"\n<font size=\"20\"><b>O seu usuário não tem permissão para registrar atividades!</b></font>\n<br/><br/>");
		}
		
		BigDecimal id = VO.asBigDecimal("ID");
		int idTratativa = getMaxIdDaTabela(id);
		VO.setProperty("IDTRATATIVA", new BigDecimal(idTratativa));
		VO.setProperty("ATENDENTE", getUsuLogado());
		
		if(idTratativa==1) {		
			cadastrarServicoPorExecutante();
			atualizaSubOs(VO);
		}else {
			cadastraSubOs(VO);
		}
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		DynamicVO oldVO = (DynamicVO) arg0.getOldVO();
		Timestamp fimAtividade = oldVO.asTimestamp("FIMATIVIDADE");
		
		String resol = VO.asString("DESCRICAO");
		
		if(!validaUsuario(getUsuLogado())) {
			throw new PersistenceException(
					"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
					"\n<font size=\"20\"><b>O seu usuário não tem permissão para alterar atividades!</b></font>\n<br/><br/>");
		}
		
		if(fimAtividade!=null && resol==null) {
			throw new PersistenceException(
					"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
					"\n<font size=\"20\"><b>Atividades não podem ser concluidas sem uma resolução!</b></font>\n<br/><br/>");
		}
		
		DynamicVO chamado = getChamado(VO.asBigDecimal("ID"));
				
		if(chamado.asTimestamp("DTFECHAMENTO")!=null) {
			throw new PersistenceException(
					"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
					"\n<font size=\"20\"><b>Chamados concluidos não podem ter suas tarefas alteradas!</b></font>\n<br/><br/>");
		}
		
		atualizaSubOs(VO);
	}
	
	private boolean validacoes(DynamicVO VO) throws Exception {
		boolean valida = false;
		BigDecimal idChamado = VO.asBigDecimal("ID");
		DynamicVO chamado = getChamado(idChamado);
		String tipo = chamado.asString("TIPO");
		BigDecimal atendimento = chamado.asBigDecimal("ATENDENTE");
		
		if(tipo==null) {
			valida=true;
		}else {valida=false;}
		
		if(atendimento==null) {
			valida=true;
		}
		
		return valida;
	}
	
	private DynamicVO getChamado(BigDecimal id) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("AD_CHAMADOTI");
		DynamicVO VO = DAO.findOne("ID=?",new Object[] { id });
		return VO;
	}
	
	@SuppressWarnings("deprecation")
	private void atualizaSubOs(DynamicVO VO) throws Exception {
		BigDecimal id = VO.asBigDecimal("ID");
		BigDecimal idTratativa = VO.asBigDecimal("IDTRATATIVA");
		Timestamp dtInicio = VO.asTimestamp("INIATIVIDADE");
		Timestamp dtFim = VO.asTimestamp("FIMATIVIDADE");
		String descricao = VO.asString("DESCRICAO");
		
		if(dtFim!=null) {
			if(dtInicio.after(dtFim)){
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
						"\n<font size=\"20\"><b>A data de ínicio não pode ser superior a data de finalização!</b></font>\n<br/><br/>");
			}
			
			int diaInicio = dtInicio.getDay();
			int diaFim = dtFim.getDay();
			
			if(diaInicio!=diaFim) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
						"\n<font size=\"20\"><b>A tarefa deve ser iniciada e finalizada no mesmo dia!</b></font>\n<br/><br/>");
			}
		}
		
		DynamicVO chamado = getChamado(id);
		BigDecimal numos = chamado.asBigDecimal("NUMOS");
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemOrdemServico",
					"this.NUMOS=? AND this.NUMITEM=? ", new Object[] { numos, idTratativa }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VOS = (DynamicVO) NVO;
				
				if(dtInicio!=null) {
					VOS.setProperty("INICEXEC", TimeUtils.clearTime(dtInicio));
					VOS.setProperty("CODUSUALTER", getUsuLogado());
					VOS.setProperty("DTALTER", new Timestamp(System.currentTimeMillis()));
					VOS.setProperty("CODUSU", getUsuLogado());
					String horaEMinuto = hrFinal(dtInicio);
					VOS.setProperty("HRINICIAL", new BigDecimal(horaEMinuto));
				}
				
				if(dtFim!=null) {
					VOS.setProperty("CODSIT", new BigDecimal(4));
					String horaEMinuto = hrFinal(dtFim);
					VOS.setProperty("HRFINAL", new BigDecimal(horaEMinuto));
					VOS.setProperty("TERMEXEC", dtFim);
				}
				
				if(descricao!=null) {
					VOS.setProperty("SOLUCAO", descricao);
				}

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[atualizaSubOs] NAO FOI POSSIVEL ALTERAR A SUB-OS OS: "+numos+e.getMessage()+"\n"+e.getCause());
		}
		
	}
	
	@SuppressWarnings("deprecation")
	private String hrFinal(Timestamp hr) {
		Integer minuto = hr.getMinutes();
		String correcao = "";
		String hrFinal = "";
		
		if(minuto>=0 && minuto<=9) {
			correcao="0"+minuto;
		}
		
		if(correcao!="") {
			hrFinal = MessageFormat.format("{0}{1}",hr.getHours(),correcao);
		}else {
			hrFinal = MessageFormat.format("{0}{1}",hr.getHours(),hr.getMinutes());
		}
		
		return hrFinal;
	}
	
	@SuppressWarnings("deprecation")
	private void cadastraSubOs(DynamicVO VO) throws Exception {
		BigDecimal id = VO.asBigDecimal("ID");
		BigDecimal idTratativa = VO.asBigDecimal("IDTRATATIVA");
		Timestamp dtInicio = VO.asTimestamp("INIATIVIDADE");
		Timestamp dtFim = VO.asTimestamp("FIMATIVIDADE");
		String descricao = VO.asString("DESCRICAO");
		
		DynamicVO chamado = getChamado(id);
		BigDecimal numos = chamado.asBigDecimal("NUMOS");
		String status = chamado.asString("STATUS");
		BigDecimal corSla = descobreCorSla(status);
		
		if(dtFim!=null) {
			if(dtInicio.after(dtFim)){
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
						"\n<font size=\"20\"><b>A data de ínicio não pode ser superior a data de finalização!</b></font>\n<br/><br/>");
			}
			
			int diaInicio = dtInicio.getDay();
			int diaFim = dtFim.getDay();
			
			if(diaInicio!=diaFim) {
				throw new PersistenceException(
						"<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
						"\n<font size=\"20\"><b>A tarefa deve ser iniciada e finalizada no mesmo dia!</b></font>\n<br/><br/>");
			}
		}

		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO ModeloNPVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("ItemOrdemServico",new Object[]{new BigDecimal(414937),new BigDecimal(1)});
			DynamicVO NotaProdVO = ModeloNPVO.buildClone();
			
			NotaProdVO.setProperty("NUMOS",numos);
			NotaProdVO.setProperty("NUMITEM",idTratativa);
			
			if(dtInicio!=null) {
				NotaProdVO.setProperty("INICEXEC", TimeUtils.clearTime(dtInicio));
				NotaProdVO.setProperty("CODUSUALTER", getUsuLogado());
				NotaProdVO.setProperty("DTALTER", new Timestamp(System.currentTimeMillis()));
				NotaProdVO.setProperty("CODUSU", getUsuLogado());
				String horaEMinuto = hrFinal(dtInicio);
				NotaProdVO.setProperty("HRINICIAL", new BigDecimal(horaEMinuto));
				NotaProdVO.setProperty("CODSIT", new BigDecimal(1));
			}
			
			if(dtFim!=null) {
				NotaProdVO.setProperty("CODSIT", new BigDecimal(4));
				String horaEMinuto = hrFinal(dtFim);
				NotaProdVO.setProperty("HRFINAL", new BigDecimal(horaEMinuto));
				NotaProdVO.setProperty("TERMEXEC", dtFim);
			}
			
			if(descricao!=null) {
				NotaProdVO.setProperty("SOLUCAO", descricao);
			}else {
				NotaProdVO.setProperty("SOLUCAO", " ");
			}

			NotaProdVO.setProperty("DHPREVISTA", addDias(dtInicio,new BigDecimal(7)));			  
			NotaProdVO.setProperty("SERIE", null);
			NotaProdVO.setProperty("CODOCOROS", new BigDecimal(55));
			NotaProdVO.setProperty("CORSLA", corSla);
			
			dwfFacade.createEntity(DynamicEntityNames.ITEM_ORDEM_SERVICO,(EntityVO) NotaProdVO);


		} catch (Exception e) {			
			salvarException("[cadastraSubOs] NAO FOI POSSIVEL GERAR A SUB-OS OS: "+numos+e.getMessage()+"\n"+e.getCause());
		}
	}
	
	private BigDecimal descobreCorSla(String status) {
		BigDecimal cor = null;
		
		switch (status) {
		
		case "1": cor=new BigDecimal(11909048); //pendente
			break;
		case "2": cor=new BigDecimal(133482); //em execucao
			break;
		case "3": cor=new BigDecimal(15308032); //em aprovacao
			break;
		case "4": cor=new BigDecimal(8570928);//concluido
			break;
		case "5": cor=new BigDecimal(2829100);//cancelado
			break;
		case "7": cor=new BigDecimal(16113568); //aguardando usuario
			break;

		default:
			break;
		}
		
		return cor;
	}
	
	private void cadastrarServicoPorExecutante() {
		try {
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("ServicoProdutoExecutante");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("CODSERV", new BigDecimal(511166));
			VO.setProperty("CODPROD", new BigDecimal(511188));
			VO.setProperty("CODUSU", getUsuLogado());
			
			dwfFacade.createEntity("ServicoProdutoExecutante", (EntityVO) VO);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}
	
	private int getMaxIdDaTabela(BigDecimal id) throws Exception {
		int count = 0;
		
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT NVL(MAX(IDTRATATIVA),0)+1 AS ID FROM AD_TRATATIVATI WHERE ID="+id);
		contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			count = contagem.getInt("ID");
		}	
					
		return count;
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
	
	private boolean validaUsuario(BigDecimal usuario) throws Exception {
		boolean valida=false;
		JapeWrapper DAO = JapeFactory.dao("Usuario");
		DynamicVO VO = DAO.findOne("CODUSU=?",new Object[] { usuario });
		String visualizaTodosOsChamados = VO.asString("AD_CHAMADOSTI");
		if("S".equals(visualizaTodosOsChamados)) {
			valida=true;
		}
		return valida;
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "evento_criaSubOs");
			VO.setProperty("PACOTE", "br.com.grancoffee.ChamadosTI");
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
