package br.com.grancoffee.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
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
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;
	// falta documentar
public class btn_statusOS implements AcaoRotinaJava {
	int x = 0;
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		if(linhas.length==1) {
			start(linhas,arg0);
		}	
		
		if(x > 0) {
			arg0.setMensagemRetorno("Status do chamado alterado!");
		}
	}
	
	private void start(Registro[] linhas,ContextoAcao arg0) throws Exception {
		String status = (String) arg0.getParam("STATUS");
		BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
		Timestamp dtEncerramento = (Timestamp) linhas[0].getCampo("DTFECHAMENTO");
		String tipo = (String) linhas[0].getCampo("TIPO");
		BigDecimal atendente = (BigDecimal) linhas[0].getCampo("ATENDENTE");
		
		//08-06-22 Inserido funcionalidade para reclassificação. ---
		String novaClassificacao = (String) arg0.getParam("CLASSIFICACAO");
		if(novaClassificacao!=null) {
			linhas[0].setCampo("CLASSIFICACAO", new BigDecimal(novaClassificacao));
		}//---
		
		//08-06-22 Inserido funcionalidade para validar se o chamado está como análise se sim não permitir. ---
		BigDecimal classificacaoAtual = (BigDecimal) linhas[0].getCampo("CLASSIFICACAO"); //1001003 - Analise
		if(classificacaoAtual.intValue()==1001003 && "4".equals(status)) {
			arg0.mostraErro("</br><b>Opps</b><br/>Não é possível encerar um chamado classificado como Análise, reclassifica-lo !");
		}//---
		
		if(tipo==null) {
			arg0.mostraErro("Chamado não foi classificado, não é possível alterar o status!");
		}
		
		if(atendente==null) {
			arg0.mostraErro("Chamado não possui um atendente, não é possível alterar o status!");
		}
		
		if(dtEncerramento!=null) {
			arg0.mostraErro("Chamado encerrado! não é possível alterar o status!");
		}else {
			validaStatus(status,numos,linhas,arg0);
		}
	}
	
	private void validaStatus(String status,BigDecimal numos,Registro[] linhas,ContextoAcao arg0) throws Exception {
		switch (status) {
		
		case "1": alteraStatusOs(status,numos,arg0,linhas); //pendente
			break;
		case "2": alteraStatusOs(status,numos,arg0,linhas); //em execucao
			break;
		case "3": alteraStatusOs(status,numos,arg0,linhas); //em aprovacao
			break;
		case "4": encerraChamado(linhas,arg0,status);//concluido
			break;
		case "5": encerraChamado(linhas,arg0,status);//cancelado
			break;
		case "7": alteraStatusOs(status,numos,arg0,linhas); //aguardando usuario
			break;
		case "8": alteraStatusOs(status,numos,arg0,linhas); //aguardando fornecedor
			break;

		default:
			break;
		}
		
		alteraCorSubOS(numos,status);
		alterarStatusNaTela(linhas,status);
		enviaEmailAvisandoAlteracaoDeStatus(linhas,status);
	}
	
	private void alteraStatusOs(String status, BigDecimal numos,ContextoAcao arg0,Registro[] linhas) throws Exception {
		
		String statusOs = (String) linhas[0].getCampo("STATUS");
		
		if("4".equals(statusOs)||"5".equals(statusOs)) {
			arg0.mostraErro("<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
					"\n <b>O chamado está concluido ou cancelado, não pode ter o status alterado!</b> \n");
		}
		
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemServico",
					"this.NUMOS=?", new Object[] { numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CODCOS", new BigDecimal(status));

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[alteraStatusOs] - NAO FOI POSSIVEL ALTERAR O STATUS DA OS! "+e.getMessage()+"\n"+e.getMessage());
		}

	}
	
	private void alterarStatusNaTela(Registro[] linhas,String status) throws Exception {
		linhas[0].setCampo("STATUS", status);
		
		if("4".equals(status)) {
			linhas[0].setCampo("DTFECHAMENTO", new Timestamp(System.currentTimeMillis()));
		}
		
		if("5".equals(status)) {
			linhas[0].setCampo("DTFECHAMENTO", new Timestamp(System.currentTimeMillis()));
			linhas[0].setCampo("CANCELADO","S");
		}
		
		x++;
	}
	
	private void encerraChamado(Registro[] linhas,ContextoAcao arg0,String status) throws Exception {
		BigDecimal id = (BigDecimal) linhas[0].getCampo("ID");
		BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
		
		if(!validaResolucao(id)) {
			arg0.mostraErro("<p align=\"center\"><img src=\"http://grancoffee.com.br/wp-content/uploads/2016/07/grancoffee-logo-325x100.png\" height=\"100\" width=\"325\"></img></p><br/>"+
					"\n <b>Não existem tarefas, ou existem tarefas pendentes, encerra-las antes de concluir o chamado!</b> \n");
		}
		
		finalizaChamadoNaTcsOse(numos,status);
	}
	
	private boolean validaResolucao(BigDecimal id) throws Exception{
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("AD_TRATATIVATI");
		DynamicVO VO = DAO.findOne("id=?",new Object[] { id });
		
		if(VO!=null) {
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT COUNT(*) FROM AD_TRATATIVATI WHERE FIMATIVIDADE IS NULL AND ID=" + id);
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				int count = contagem.getInt("COUNT(*)");
				if (count == 0) {
					valida = true;
				}
			}
		}
		
		return valida;

	}
	
	private void finalizaChamadoNaTcsOse(BigDecimal numos,String status) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("OrdemServico",
					"this.NUMOS=?", new Object[] { numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("SITUACAO", "F");
				VO.setProperty("DTFECHAMENTO", new Timestamp(System.currentTimeMillis()));
				VO.setProperty("CODUSUFECH", getUsuLogado());
				VO.setProperty("DHFECHAMENTOSLA", new Timestamp(System.currentTimeMillis()));
				VO.setProperty("CODCOS", new BigDecimal(status));

				itemEntity.setValueObject(NVO);
			}
			
		} catch (Exception e) {
			salvarException("[finalizaChamadoNaTcsOse] - NAO FOI POSSIVEL FINALIZAR O CHAMADO! "+e.getMessage()+"\n"+e.getMessage());
		}
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}
	
	private void alteraCorSubOS(BigDecimal numos, String status) {
		
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
		case "8": cor=new BigDecimal(16113568); //aguardando Fornecedor
			break;
		default:
			break;
		}
		
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("ItemOrdemServico",
					"this.NUMOS=?", new Object[] { numos }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("CORSLA", cor);

				itemEntity.setValueObject(NVO);
			}

		} catch (Exception e) {
			//salvarException("[alteraCorSubOS] - NAO FOI POSSIVEL ALTERAR A COR DO SLA! "+e.getMessage()+"\n"+e.getMessage());
		}
	}
	
	private void enviaEmailAvisandoAlteracaoDeStatus(Registro[] linhas,String status) {
		String statusAtual = "";
		String textoComplementar = "";
		
		switch (status) {
		
		case "1": statusAtual="PENDENTE";
				  textoComplementar="O seu chamado está na fila de atendimento, verificar a data prevista de atendimento na tela Chamados TI.";//pendente
			break;
		case "2": statusAtual="EM EXECUÇÃO";
				  textoComplementar="O seu chamado está sendo atendido, fique atento as comunicações e tratativas na tela Chamados TI.";//em execucao
			break;
		case "3": statusAtual="EM APROVAÇÃO"; 
				  textoComplementar="O seu chamado foi Atendido, porém está aguardando aprovação, verificar as tratativas na tela Chamados TI.";//em aprovacao
			break;
		case "4": statusAtual="CONCLUIDO";
				  textoComplementar="O seu chamado foi Finalizado, verificar as tratativas na tela Chamados TI.\n O que achou do atendimento ? de sua opinião: <a href=\"https://forms.office.com/Pages/ResponsePage.aspx?id=oJBZB5UVOUWeyfQH8I-ZsciCmKe9i-lAuAx2UMlzcq1UM0VBMlZUTFVDVjZSTkxYT1ozOUpXVkNTTi4u\"> clique aqui.</a>";//concluido
			break;
		case "5": statusAtual="CANCELADO";
				  textoComplementar="O seu chamado foi Cancelado, verificar o motivo nas tratativas na tela Chamados TI.";//cancelado
			break;
		case "7": statusAtual="AGUARDANDO USUARIO"; 
				  textoComplementar="Para a continuidade do atendido é necessário responder algum questionamento do setor de TI, verificar as comunicações na tela Chamados TI.";//aguardando usuario
			break;
		case "8": statusAtual="AGUARDANDO FORNECEDOR"; 
		  textoComplementar="O seu chamado está dependendo de um fornecedor externo, verificar as informações na tela Chamados TI.";//aguardando usuario
		  break;

		default:
			break;
		}
		
		String email = (String) linhas[0].getCampo("EMAIL");
		BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
		String descricao = StringUtils.substr(linhas[0].getCampo("DESCRICAO").toString(), 0, 100);
		
		try {
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "O seu chamado de número <b>"+numos+"</b>."
					+ "<br/><br/><i>\""+descricao+" ...\"</i>"
					+ "<br/><br/>teve o seu status alterado."
					+ "<br/><br/><b>Status Atual:</b> "+statusAtual
					+ "<br/><br/>Isso significa que: <i> "+textoComplementar+"</i>"
					+ "<br/><br/><b>Esta é uma mensagem automática, por gentileza não respondê-la</b>"
					+ "<br/><br/>Atencionamente,"
					+ "<br/>Departamento TI"
					+ "<br/>Gran Coffee Comércio, Locação e Serviços S.A."
					+ "<br/>"
					+ "<img src=https://grancoffee.com.br/wp-content/themes/gran-coffe/assets/img/logo-gran-coffee-black.svg  alt=\"\"/>";
			
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
			VO.setProperty("CODSMTP", getContaSmtpPrincipal());
			VO.setProperty("MAXTENTENVIO", new BigDecimal(3));
			VO.setProperty("TENTENVIO", new BigDecimal(0));
			VO.setProperty("REENVIAR", "N");
			
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
		} catch (Exception e) {
			salvarException("[enviaEmailAvisandoAlteracaoDeStatus] - NAO FOI POSSIVEL ENVIAR E-MAIL INFORMANDO O STATUS! "+e.getMessage()+"\n"+e.getMessage());
		}	
	}
	
	private BigDecimal getContaSmtpPrincipal() throws Exception {
		int count = 1;

		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODSMTP) AS COD FROM TSISMTP WHERE PADRAO = 'S'");
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			count = contagem.getInt("COD");
		}

		BigDecimal codigoConta = new BigDecimal(count);

		return codigoConta;
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
	
	private void salvarException(String mensagem) {
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;
			
			VO.setProperty("OBJETO", "btn_statusOS");
			VO.setProperty("PACOTE", "br.com.grancoffee.ChamadosTI");
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
