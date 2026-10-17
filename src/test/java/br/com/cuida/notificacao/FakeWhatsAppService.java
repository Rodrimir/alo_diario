package br.com.cuida.notificacao;

import br.com.cuida.notificacao.model.MensagemWhatsAppDTO;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * WhatsApp dos testes (padrão do {@code FakeEmailService} do projeto de referência): guarda as
 * mensagens "enviadas" para os testes conferirem e pode ser configurado para falhar.
 */
@Service
@Profile("test")
public class FakeWhatsAppService implements WhatsAppService {

  private final List<MensagemWhatsAppDTO> enviadas = new CopyOnWriteArrayList<>();
  private volatile boolean falhar;

  @Override
  public String enviarTemplate(MensagemWhatsAppDTO mensagem) {
    if (falhar) {
      throw new EnvioWhatsAppException("falha simulada no whats-api");
    }
    enviadas.add(mensagem);
    return "fake-" + UUID.randomUUID();
  }

  public List<MensagemWhatsAppDTO> enviadasPara(String destino) {
    return enviadas.stream().filter(m -> m.destino().equals(destino)).toList();
  }

  public void falhar(boolean falhar) {
    this.falhar = falhar;
  }
}
