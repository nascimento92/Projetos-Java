package br.com.gsn.financeiro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.SWRepositoryUtils;
import br.com.sankhya.ws.ServiceContext;

public class btn_enviaNotificacaoExtraJudicial implements AcaoRotinaJava {

	private int quantidadeDeEmailsEnviados = 0;
	private int numeroDaLinha = 0;

	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		lerPlanilha(arg0);
		apagarPlanilha(arg0);
		arg0.setMensagemRetorno(
				"Total de  " + String.valueOf(this.quantidadeDeEmailsEnviados) + "  notificaenviadas com sucesso!");
	}

	public void apagarPlanilha(ContextoAcao arg0) throws Exception {
		File repo = SWRepositoryUtils.getBaseFolder();
		File workFolder = new File(repo, "NotificacaoExtraJudicial");
		File f = new File(workFolder, "Notificacao.csv");
		f.delete();
	}

	public void lerPlanilha(ContextoAcao arg0) throws Exception {
		File repo = SWRepositoryUtils.getBaseFolder();
		File workFolder = new File(repo, "NotificacaoExtraJudicial");
		String codigoDoPaceiro = null;
		String nomeDoParceiro = null;
		String numeroDaNota = null;
		String dataDeNegociacao = null;
		String dataDeVencimento = null;
		String valorDoDesdobramento = null;
		String cnpjDoParceiro = null;
		String enderecoCompletoDoParceiro = null;
		String emailDoParceiro = null;
		workFolder.mkdirs();
		try {
			File f = new File(workFolder, "Notificacao.csv");
			if (f.exists()) {
				FileReader fr = new FileReader(f);
				BufferedReader br = new BufferedReader(fr);
				while (br.ready()) {
					this.numeroDaLinha++;
					String linha = br.readLine();
					String[] values = linha.split("[;]");
					try {
						codigoDoPaceiro = values[0];
						nomeDoParceiro = values[1];
						numeroDaNota = values[2];
						dataDeNegociacao = values[3];
						dataDeVencimento = values[4];
						valorDoDesdobramento = values[5];
						System.out.println(codigoDoPaceiro);
					} catch (Exception ex) {
						arg0.mostraErro(
								"Erro ao ler a planilha.<br> Verifique se existem valores nulos. <br><br> Linha: "
										+ this.numeroDaLinha + " - " + ex);
					}
					try {
						cnpjDoParceiro = getCnpjDaNotificada(codigoDoPaceiro);
						enderecoCompletoDoParceiro = getEnderecoCompletoDaNotificada(values[0]);
						emailDoParceiro = getEmailDaNotificada(values[0]);
					} catch (Exception e) {
						arg0.mostraErro("Erro na busca de valores: " + e);
					}
					if (validaDado(codigoDoPaceiro, nomeDoParceiro, numeroDaNota, dataDeNegociacao, dataDeVencimento,
							valorDoDesdobramento, cnpjDoParceiro, enderecoCompletoDoParceiro, emailDoParceiro) == 0) {
						enviarEmailDeNotificacaoExtrajudicial(nomeDoParceiro, numeroDaNota, dataDeNegociacao,
								dataDeVencimento, valorDoDesdobramento, cnpjDoParceiro, enderecoCompletoDoParceiro,
								emailDoParceiro, arg0.getUsuarioLogado());
						continue;
					}
					arg0.mostraErro(
							"Foi encontrado valor nulo, em branco ou com formato incorreto no arquivo. Parceiro: "
									+ codigoDoPaceiro);
				}
				br.close();
				fr.close();
			} else {
				arg0.mostraErro("Arquivo Notificacao.csv nencontrado no Repositorio de Arquivos!");
			}
		} catch (IOException e) {
			salvarException("[lerPlanilha] erro ao ler o arquivo"+e+"\n"+e.getMessage()+"\n"+e.getCause());
		}
	}

	private int validaDado(String codigoDoPaceiro, String nomeDoParceiro, String numeroDaNota, String dataDeNegociacao,
			String dataDeVencimento, String valorDoDesdobramento, String cnpj, String endereco, String email) {
		int valida = 0;
		try {
			if (codigoDoPaceiro == null)
				valida++;
			if (nomeDoParceiro == null)
				valida++;
			if (numeroDaNota == null)
				valida++;
			if (dataDeNegociacao == null)
				valida++;
			if (dataDeVencimento == null)
				valida++;
			if (valorDoDesdobramento == null)
				valida++;
			if (cnpj == null)
				valida++;
			if (endereco == null)
				valida++;
			if (email == null)
				valida++;
			try {
				if (!verificarValor(valorDoDesdobramento.replace(".", "").replace(",", ".")))
					valida++;
			} catch (NumberFormatException ex) {
				System.out.println("##Notificacao Extrajudicial## campo valor do desdobramento com valor texto");
				valida++;
			}
			if (codigoDoPaceiro != null && codigoDoPaceiro.isEmpty())
				valida++;
			if (nomeDoParceiro != null && nomeDoParceiro.isEmpty())
				valida++;
			if (numeroDaNota != null && numeroDaNota.isEmpty())
				valida++;
			if (dataDeNegociacao != null && dataDeNegociacao.isEmpty())
				valida++;
			if (dataDeVencimento != null && dataDeVencimento.isEmpty())
				valida++;
			if (valorDoDesdobramento != null && valorDoDesdobramento.isEmpty())
				valida++;
			if (cnpj != null && cnpj.isEmpty())
				valida++;
			if (endereco != null && endereco.isEmpty())
				valida++;
			if (email != null && email.isEmpty())
				valida++;
		} catch (NullPointerException e) {
			valida++;
			salvarException("[validaDado] nao foi possivel validar o dado numero da nota: "+numeroDaNota+"\n"+e.getMessage()+"\n"+e.getCause());			
		}
		System.out.println("Valida Dados" + valida);
		return valida;
	}

	private String getCnpjDaNotificada(String codigoDoCliente) throws Exception {
		String cnpjDoParceiro = null;
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT CGC_CPF FROM TGFPAR WHERE CODPARC = " + Integer.parseInt(codigoDoCliente));
		ResultSet contagem = nativeSql.executeQuery();
		while (contagem.next())
			cnpjDoParceiro = contagem.getString("CGC_CPF");
		return cnpjDoParceiro;
	}

	private String getEmailDaNotificada(String codigoDoCliente) throws Exception {
		String emailDoParceiro = null;
		BigDecimal codigoDoParceiro = new BigDecimal(Integer.parseInt(codigoDoCliente));
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT EMAIL FROM TGFPAR WHERE CODPARC = " + codigoDoParceiro);
		ResultSet contagem = nativeSql.executeQuery();
		while (contagem.next())
			emailDoParceiro = contagem.getString("EMAIL");
		return emailDoParceiro;
	}

	private String getEnderecoCompletoDaNotificada(String codigoDoCliente) throws Exception {
		String enderecoDoParceiro = null;
		BigDecimal codigoDoParceiro = new BigDecimal(Integer.parseInt(codigoDoCliente));
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql(
				"SELECT CODPARC, TRIM(E.TIPO) || ' ' || TRIM(NOMEEND) || ', ' || TRIM(P.NUMEND) || ' - '||  TRIM(P.COMPLEMENTO) || ' ,' || TRIM(B.NOMEBAI) || ' - ' || TRIM(C.NOMECID) || '/' || TRIM(U.UF) || ' - '||TRIM(P.CEP) AS ENDERECO\r\nFROM TGFPAR P\r\nINNER JOIN TSICID C ON (P.CODCID = C.CODCID)\r\nINNER JOIN TSIBAI B ON (P.CODBAI = B.CODBAI)\r\nINNER JOIN TSIEND E ON (P.CODEND = E.CODEND)\r\nINNER JOIN TSIUFS U ON (U.CODUF = C.UF)\r\nWHERE P.CODPARC = "
						+

						codigoDoParceiro);
		ResultSet contagem = nativeSql.executeQuery();
		while (contagem.next()) {
			enderecoDoParceiro = contagem.getString("ENDERECO");
			System.out.println("Endereco: " + enderecoDoParceiro);
		}
		return enderecoDoParceiro;
	}

	private BigDecimal getUltimoCodigoFila() throws Exception {
		int count = 0;
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(CODFILA)+1 AS CODFILA FROM TMDFMG");
		ResultSet contagem = nativeSql.executeQuery();
		while (contagem.next())
			count = contagem.getInt("CODFILA");
		BigDecimal ultimoCodigo = new BigDecimal(count);
		return ultimoCodigo;
	}

	public static boolean verificarValor(String valor) {
		try {
			double d = Double.parseDouble(valor);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void enviarEmailDeNotificacaoExtrajudicial(String nomeDoParceiro, String numeroDaNota,
			String dataDeNegociacao, String dataDeVencimento, String valorDoDesdobramento, String cnpjDoParceiro,
			String enderecoDoParceiro, String emailDoParceiro, BigDecimal usuarioLogado) throws Exception {
		try {
			String mensagemCompleta = new String();
			mensagemCompleta = "<h3>NOTIFICAÇÃO EXTRAJUDICIAL</h3> NOTIFICANTE GRAN COFFEE COMÉRCIO, LOCAÇÃO E SERVIÇOS LTDA, CNPJ: 08.736.011/0001-46 e Rua Manoel Veiga, 58 - Campinas/SP, por intermédio de seus representantes legais, vem, por meio desta, apresentar a seguinte NOTIFICAÇÃO EXTRAJUDICIAL pelos fatos e motivos abaixo aduzidos.<br><br><b>COBRANÇA DE VALORES</b> <br> <br>Em análise, não identificamos o(s) pagamento(s) dos(s) valor(es) abaixo: <br> <br> <b> Notificada: </b> "
					+nomeDoParceiro + "<br> <b> Endereço: </b> " + enderecoDoParceiro 
					+ "<br> <b> CNPJ: </b> "+cnpjDoParceiro 
					+ "<br> <b> Número da Nota: </b> " + numeroDaNota 
					+ "<br> <b> Data de emissão</b> "+ dataDeNegociacao 
					+ "<br> <b> Data de Vencimento: </b> " + dataDeVencimento
					+ "<br> <b> Valor: </b> R$ " + valorDoDesdobramento
					+ "<br> <br> Prezando pelos princípios da boa-fé contratual"
					+ " e da parceria empresarial, a Notificada serve-se desta, manifestando "
					+ "seu interesse de contribuir célere, amigável e pacificamente para o "
					+ "recebimento do valor acima descrito. Caso desejar realizar o pagamento"
					+ " pelo(s) boleto(s), entrar em contato através do e-mail "
					+ "contasareceber@grancoffee.com.br. Sendo ou não identificada a "
					+ "quitação do valor acima disposto, solicitamos o contato via e-mail, "
					+ "inserindo no assunto o termo " + nomeDoParceiro + " - " + cnpjDoParceiro
					+ " ,no prazo de 05 (cinco) dias corridos, contados do recebimento "
					+ "desta, para as devidas tratativas. Na ausência de resposta no prazo "
					+ "acima estipulado, fica a Notificada, ciente da possibilidade de "
					+ "registro de protesto junto ao cartório competente, e consequentemente "
					+ "aos órgãos de serviço de proteção ao crédito - SCPC e Serasa. "
					+ "A Notificante aproveita-se da oportunidade para renovar os votos de "
					+ "elevada estima e consideração à Notificada, colocando-se disposição"
					+ "para eventuais esclarecimentos, aguardando resposta para proceder à"
					+ "resolução célere, amigável e pacífica do conflito existente, deixando "
					+ "de proceder à judicialização nos moldes legais, neste momento. " + "<br> <br> Sem mais, "
					+ "<br><br> <b>GRAN COFFEE COMÉRCIO, LOCAÇÃO E SERVIÇO LTDA</b>";
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
			DynamicVO VO = (DynamicVO) NPVO;
			VO.setProperty("CODFILA", getUltimoCodigoFila());
			VO.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("MENSAGEM", mensagemCompleta.toCharArray());
			VO.setProperty("TIPOENVIO", "E");
			VO.setProperty("ASSUNTO", new String("NOTIFICAÇÃO EXTRAJUDICIAL"));
			VO.setProperty("EMAIL", emailDoParceiro);
			VO.setProperty("CODUSU", new BigDecimal(0));
			VO.setProperty("STATUS", "Pendente");
			VO.setProperty("CODCON", new BigDecimal(0));
			VO.setProperty("CODSMTP", getContaSmtpPrincipal());
			VO.setProperty("MAXTENTENVIO", new BigDecimal(3));
			VO.setProperty("TENTENVIO", new BigDecimal(0));
			VO.setProperty("REENVIAR", "N");
			VO.setProperty("CODUSU", usuarioLogado);
			dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) VO);
			this.quantidadeDeEmailsEnviados++;
		} catch (Exception e) {
			salvarException("[enviarEmailDeNotificacaoExtrajudicial] nao foi possivel enviar o e-mail numero da nota: "+numeroDaNota+"\n e-mail: "+emailDoParceiro+"\n"+e.getMessage()+e.getCause());
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
	
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "btn_enviaNotificacaoExtraJudicial");
			VO.setProperty("PACOTE", "br.com.gsn.financeiro");
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
