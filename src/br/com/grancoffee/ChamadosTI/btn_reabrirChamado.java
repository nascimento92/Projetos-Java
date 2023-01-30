package br.com.grancoffee.ChamadosTI;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import com.sankhya.util.StringUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.PersistenceError;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_reabrirChamado implements AcaoRotinaJava {
	
	/**
	 * 30/01/2023 - Gabriel Nascimento - Botão será inativado, pois as solicitações serão tratadas via flow.
	 */
	int cont=0;
	String retorno="";
	
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();

		if (linhas.length == 1) {
			start(linhas, arg0);
		} else {
			throw new PersistenceError("Selecione apenas um chamado!");
		}
		
		if(cont>0) {
			arg0.setMensagemRetorno("Chamado Reaberto!");
		}else {
			arg0.setMensagemRetorno("ERRO! - \n"+retorno);
		}
	}

	private void start(Registro[] linhas, ContextoAcao arg0) throws Exception {
		BigDecimal numos = (BigDecimal) linhas[0].getCampo("NUMOS");
		String status = (String) linhas[0].getCampo("STATUS");
		String email = (String) linhas[0].getCampo("EMAIL");
		String descricao = StringUtils.substr(linhas[0].getCampo("DESCRICAO").toString(), 0, 100);

		if ("4".equals(status)) {
			reabrirOS(numos);
			alterarSubOs(numos);
			alteraInformacoesLocais(linhas);
			//enviarEmail(numos,descricao,email);
		} else {
			throw new PersistenceError("Chamado não está concluído, não pode ser reaberto!");
		}
	}

	private void alteraInformacoesLocais(Registro[] linhas) throws Exception {
		linhas[0].setCampo("STATUS", "1");
		linhas[0].setCampo("DTFECHAMENTO", null);
		linhas[0].setCampo("REABERTO", "S");
		BigDecimal id = (BigDecimal) linhas[0].getCampo("ID");
		
		if(linhas[0].getCampo("DTFECHAMENTO")==null) {
			limpaResolucoes(id);
		}
	}

	private void limpaResolucoes(BigDecimal id) throws Exception {
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("AD_TRATATIVATI", "this.ID=?", new Object[] { id }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				//VO.setProperty("INIATIVIDADE", null);
				VO.setProperty("FIMATIVIDADE", null);
				VO.setProperty("TIPOATENDIMENTO", null);

				String descricaoAnterior = VO.asString("DESCRICAO");
				VO.setProperty("DESCRICAO", "CHAMADO REABERTO\n" + descricaoAnterior);

				itemEntity.setValueObject(NVO);
			}
		} catch (Exception e) {
			System.out.println("## [btnReabrirChamado] ## - Nao foi possivel alterar as tratativas!");
			e.getMessage();
			e.getCause();
			retorno += e.getMessage();
		}
	}

	private void reabrirOS(BigDecimal numos) throws Exception {

		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("OrdemServico", "this.NUMOS=?", new Object[] { numos }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("SITUACAO", "P");
				VO.setProperty("DTFECHAMENTO", null);
				VO.setProperty("CODUSUFECH", null);
				VO.setProperty("DHFECHAMENTOSLA", null);
				VO.setProperty("CODCOS", new BigDecimal(1));

				itemEntity.setValueObject(NVO);
			}

		} catch (Exception e) {
			System.out.println("## [btnReabrirChamado] ## - Nao foi possivel reabrir a OS!");
			e.getMessage();
			e.getCause();
			retorno += e.getMessage();
		}

	}

	private void alterarSubOs(BigDecimal numos) throws Exception {

		try {

			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade
					.findByDynamicFinder(new FinderWrapper("ItemOrdemServico", "this.NUMOS=?", new Object[] { numos }));

			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
				PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
				EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
				DynamicVO VO = (DynamicVO) NVO;

				VO.setProperty("HRINICIAL", null);
				VO.setProperty("HRFINAL", null);
				VO.setProperty("INICEXEC", null);
				VO.setProperty("TERMEXEC", null);
				VO.setProperty("TEMPGASTO", null);
				VO.setProperty("CODSIT", new BigDecimal(1));

				itemEntity.setValueObject(NVO);
				cont++;
			}

		} catch (Exception e) {
			System.out.println("## [btnReabrirChamado] ## - Nao foi possivel reabrir a sub-OS!");
			e.getMessage();
			e.getCause();
			retorno += e.getMessage();
		}
	}
	
	private void enviarEmail(BigDecimal numos, String descricao, String email) {
		try {
			String mensagem = new String();
			
			mensagem = "Prezado,<br/><br/> "
					+ "O seu chamado de número <b>"+numos+"</b>."
					+ "<br/><br/><i>\""+descricao+" ...\"</i>"
					+ "<br/><br/>Foi reaberto!"
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
			System.out.println("## [btn_reaberirChamado] ## - Nao foi possivel enviar e-mail"+e.getMessage());
			e.printStackTrace();
			retorno += e.getMessage();
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
}
